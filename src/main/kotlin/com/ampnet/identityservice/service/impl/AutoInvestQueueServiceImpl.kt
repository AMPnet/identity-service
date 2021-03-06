package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.blockchain.IInvestService.InvestmentRecord
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.controller.pojo.request.AutoInvestRequest
import com.ampnet.identityservice.controller.pojo.response.AutoInvestResponse
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException
import com.ampnet.identityservice.persistence.model.AutoInvestTask
import com.ampnet.identityservice.persistence.model.AutoInvestTaskHistoryStatus
import com.ampnet.identityservice.persistence.model.AutoInvestTaskStatus
import com.ampnet.identityservice.persistence.model.AutoInvestTransaction
import com.ampnet.identityservice.persistence.repository.AutoInvestTaskRepository
import com.ampnet.identityservice.persistence.repository.AutoInvestTransactionRepository
import com.ampnet.identityservice.service.AutoInvestQueueService
import com.ampnet.identityservice.service.ScheduledExecutorServiceProvider
import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import com.ampnet.identityservice.util.ChainId
import com.ampnet.identityservice.util.ContractAddress
import com.ampnet.identityservice.util.ContractVersion
import com.ampnet.identityservice.util.TransactionHash
import com.ampnet.identityservice.util.WalletAddress
import mu.KLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@Service
class AutoInvestQueueServiceImpl(
    private val autoInvestTaskRepository: AutoInvestTaskRepository,
    private val autoInvestTransactionRepository: AutoInvestTransactionRepository,
    private val uuidProvider: UuidProvider,
    private val timeProvider: ZonedDateTimeProvider,
    private val blockchainService: BlockchainService,
    private val applicationProperties: ApplicationProperties,
    scheduledExecutorServiceProvider: ScheduledExecutorServiceProvider
) : AutoInvestQueueService, DisposableBean {

    companion object : KLogging() {
        const val QUEUE_NAME = "AutoInvestQueue"
        val supportedVersion = ContractVersion("1.0.20")
    }

    private val executorService = scheduledExecutorServiceProvider.newSingleThreadScheduledExecutor(QUEUE_NAME)

    init {
        if (applicationProperties.autoInvest.enabled) {
            executorService.scheduleAtFixedRate(
                { processTasks() },
                applicationProperties.autoInvest.queue.initialDelay,
                applicationProperties.autoInvest.queue.polling,
                TimeUnit.MILLISECONDS
            )
        }
    }

    override fun destroy() {
        logger.info { "Shutting down auto-invest queue executor service..." }
        executorService.shutdown()
    }

    override fun createOrUpdateAutoInvestTask(
        address: WalletAddress,
        campaign: ContractAddress,
        chainId: ChainId,
        request: AutoInvestRequest
    ): AutoInvestResponse? {
        val version = blockchainService.getContractVersion(chainId, campaign)
            ?: throw InvalidRequestException(
                ErrorCode.BLOCKCHAIN_UNSUPPORTED_VERSION,
                "This campaign is missing version number"
            )
        if (version < supportedVersion) {
            throw InvalidRequestException(
                ErrorCode.BLOCKCHAIN_UNSUPPORTED_VERSION,
                "This campaign does not support auto invest functionality"
            )
        }

        val numModified = autoInvestTaskRepository.createOrUpdate(
            AutoInvestTask(
                chainId = chainId.value,
                userWalletAddress = address.value,
                campaignContractAddress = campaign.value,
                amount = request.amount,
                status = AutoInvestTaskStatus.PENDING,
                uuidProvider = uuidProvider,
                timeProvider = timeProvider
            )
        )

        return if (numModified == 0) {
            logger.warn {
                "Auto-invest already in process for address: $address, campaign: $campaign, chainId: $chainId"
            }
            null
        } else {
            autoInvestTaskRepository.findByUserWalletAddressAndCampaignContractAddressAndChainId(
                userWalletAddress = address.value,
                campaignContractAddress = campaign.value,
                chainId = chainId.value
            )?.let {
                logger.info { "Submitted auto-invest task: $it" }
                AutoInvestResponse(
                    walletAddress = it.userWalletAddress,
                    campaignAddress = it.campaignContractAddress,
                    amount = it.amount
                )
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processTasks() {
        autoInvestTaskRepository.findByStatus(AutoInvestTaskStatus.IN_PROCESS).groupedByChainId().forEach {
            try {
                handleInProcessTasksForChain(it.key, it.value)
            } catch (ex: Throwable) {
                logger.error("Failed to handle in process tasks (chainId: ${it.key}): ${ex.message}")
            }
        }

        autoInvestTaskRepository.findByStatus(AutoInvestTaskStatus.PENDING).groupedByChainId().forEach {
            try {
                handlePendingTasksForChain(it.key, it.value)
            } catch (ex: Throwable) {
                logger.error("Failed to handle pending task (chainId: ${it.key}): ${ex.message}")
            }
        }
    }

    private fun handleInProcessTasksForChain(chainId: ChainId, tasks: List<AutoInvestTask>) {
        logger.debug { "Processing in process auto-investments for chainId: $chainId for tasks: $tasks" }
        val groupedByHash = tasks.filter { it.hash != null }.groupBy { it.hash!! }
        groupedByHash.forEach { handleInProcessHashForChain(TransactionHash(it.key), chainId, it.value) }
    }

    private fun handleInProcessHashForChain(hash: TransactionHash, chainId: ChainId, tasks: List<AutoInvestTask>) {
        if (blockchainService.isMined(hash, chainId)) {
            logger.info { "Transaction is mined: $hash, removing associated tasks" }
            autoInvestTaskRepository.completeTasks(
                tasks.map { it.uuid },
                AutoInvestTaskHistoryStatus.SUCCESS,
                ZonedDateTime.now()
            )
        } else {
            val transaction = autoInvestTransactionRepository.findByChainIdAndHash(chainId.value, hash.value)
            if (transaction?.createdAt?.isBefore(getMaximumMiningPeriod()) == true) {
                logger.warn {
                    "Waiting for transaction: $hash exceeded ${applicationProperties.autoInvest.queue.miningPeriod}" +
                        " minutes"
                }
                autoInvestTaskRepository.completeTasks(
                    tasks.map { it.uuid },
                    AutoInvestTaskHistoryStatus.FAILURE,
                    ZonedDateTime.now()
                )
            } else {
                logger.info { "Waiting for transaction to be mined: $hash" }
            }
        }
    }

    @Suppress("ReturnCount")
    private fun handlePendingTasksForChain(chainId: ChainId, tasks: List<AutoInvestTask>) {
        logger.debug { "Processing pending auto-investments for chainId: $chainId" }
        val (expiredTasks, activeTasks) = tasks.partition { it.createdAt.isBefore(getMaximumPendingPeriod()) }
        if (expiredTasks.isNotEmpty()) {
            autoInvestTaskRepository.completeTasks(
                expiredTasks.map { it.uuid },
                AutoInvestTaskHistoryStatus.EXPIRED,
                ZonedDateTime.now()
            )
            logger.info { "Expired tasks: ${expiredTasks.size}" }
        }

        val records = activeTasks.map { it.toRecord() }
        if (records.isEmpty()) {
            return
        }
        logger.debug { "Active tasks for chainId: $chainId has size: ${records.size}" }

        val statuses = blockchainService.getAutoInvestStatus(records, chainId)
        val readyToInvestTasks = activeTasks.zip(statuses).filter { it.second.readyToInvest }
        if (readyToInvestTasks.isEmpty()) {
            return
        }

        val hash = blockchainService.autoInvestFor(readyToInvestTasks.map { it.second }, chainId)
        if (hash == null || hash.value.isEmpty()) {
            logger.warn { "Failed to get hash for auto-invest for chainId: $chainId" }
            return
        }

        logger.info { "Auto-invested finished with hash: $hash for chainId: $chainId" }
        autoInvestTaskRepository.updateStatusAndHashForIds(
            readyToInvestTasks.map { it.first.uuid },
            AutoInvestTaskStatus.IN_PROCESS,
            hash.value
        )
        autoInvestTransactionRepository.saveAndFlush(
            AutoInvestTransaction(
                chainId = chainId.value,
                hash = hash.value,
                uuidProvider = uuidProvider,
                timeProvider = timeProvider
            )
        )
        logger.debug { "Update database" }
    }

    private fun getMaximumPendingPeriod() = timeProvider.getZonedDateTime()
        .minus(applicationProperties.autoInvest.timeout)

    private fun getMaximumMiningPeriod() = timeProvider.getZonedDateTime()
        .minusSeconds(applicationProperties.autoInvest.queue.miningPeriod)

    private fun List<AutoInvestTask>.groupedByChainId(): Map<ChainId, List<AutoInvestTask>> =
        groupBy { ChainId(it.chainId) }

    private fun AutoInvestTask.toRecord() =
        InvestmentRecord(this.userWalletAddress, this.campaignContractAddress, this.amount)
}
