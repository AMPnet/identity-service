package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.blockchain.IInvestService.InvestmentRecord
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.controller.pojo.request.AutoInvestRequest
import com.ampnet.identityservice.controller.pojo.response.AutoInvestResponse
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
    }

    private val executorService = scheduledExecutorServiceProvider.newSingleThreadScheduledExecutor(QUEUE_NAME)

    init {
        if (applicationProperties.autoInvest.processingEnabled) {
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
        address: String,
        campaign: String,
        chainId: Long,
        request: AutoInvestRequest
    ): AutoInvestResponse? {
        val numModified = autoInvestTaskRepository.createOrUpdate(
            AutoInvestTask(
                chainId = chainId,
                userWalletAddress = address,
                campaignContractAddress = campaign,
                amount = request.amount,
                status = AutoInvestTaskStatus.PENDING,
                uuidProvider = uuidProvider,
                timeProvider = timeProvider
            )
        )

        return if (numModified == 0) {
            logger.warn {
                "Auto-invest already in process for address: $address, campaign: $campaign," +
                    " chainId: $chainId"
            }
            null
        } else {
            val task = autoInvestTaskRepository.findByUserWalletAddressAndCampaignContractAddressAndChainId(
                userWalletAddress = address,
                campaignContractAddress = campaign,
                chainId = chainId
            )

            task?.let {
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
                logger.error("Failed to handle in process tasks (chainId: ${it.value}): ${ex.message}")
            }
        }

        autoInvestTaskRepository.findByStatus(AutoInvestTaskStatus.PENDING).groupedByChainId().forEach {
            try {
                handlePendingTasksForChain(it.key, it.value)
            } catch (ex: Throwable) {
                logger.error("Failed to handle pending task (chainId: ${it.value}): ${ex.message}")
            }
        }
    }

    private fun handleInProcessTasksForChain(chainId: Long, tasks: List<AutoInvestTask>) {
        logger.debug { "Processing in process auto-investments for chainId: $chainId" }
        val groupedByHash = tasks.filter { it.hash != null }.groupBy { it.hash!! }
        groupedByHash.forEach { handleInProcessHashForChain(it.key, chainId, it.value) }
    }

    private fun handleInProcessHashForChain(hash: String, chainId: Long, tasks: List<AutoInvestTask>) {
        if (blockchainService.isMined(hash, chainId)) {
            logger.info { "Transaction is mined: $hash, removing associated tasks" }
            autoInvestTaskRepository.completeTasks(
                tasks.map { it.uuid },
                AutoInvestTaskHistoryStatus.SUCCESS,
                ZonedDateTime.now()
            )
        } else {
            val transaction = autoInvestTransactionRepository.findByChainIdAndHash(chainId, hash)
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

    private fun handlePendingTasksForChain(chainId: Long, tasks: List<AutoInvestTask>) {
        logger.debug { "Processing pending auto-investments for chainId: $chainId" }
        val (expiredTasks, activeTasks) = tasks.partition { it.createdAt.isBefore(getMaximumPendingPeriod()) }

        logger.debug { "Deleting ${expiredTasks.size} expired tasks for chainId: $chainId" }
        autoInvestTaskRepository.completeTasks(
            expiredTasks.map { it.uuid },
            AutoInvestTaskHistoryStatus.EXPIRED,
            ZonedDateTime.now()
        )

        val records = activeTasks.map { it.toRecord() }

        if (records.isEmpty()) {
            logger.debug { "No active tasks for chainId: $chainId" }
            return
        }

        val statuses = blockchainService.getAutoInvestStatus(records, chainId)
        val readyToInvestTasks = activeTasks.zip(statuses).filter { it.second.readyToInvest }

        val hash = blockchainService.autoInvestFor(readyToInvestTasks.map { it.first.toRecord() }, chainId)

        if (hash.isNullOrEmpty()) {
            logger.warn { "Failed to get hash for auto-invest for chainId: $chainId" }
            return
        }

        logger.info { "Auto-investing for chainId: $chainId" }
        autoInvestTaskRepository.updateStatusAndHashForIds(
            readyToInvestTasks.map { it.first.uuid },
            AutoInvestTaskStatus.IN_PROCESS,
            hash
        )
        autoInvestTransactionRepository.saveAndFlush(
            AutoInvestTransaction(
                chainId = chainId,
                hash = hash,
                uuidProvider = uuidProvider,
                timeProvider = timeProvider
            )
        )
    }

    private fun getMaximumPendingPeriod() = timeProvider.getZonedDateTime()
        .minus(applicationProperties.autoInvest.timeout)

    private fun getMaximumMiningPeriod() = timeProvider.getZonedDateTime()
        .minusSeconds(applicationProperties.autoInvest.queue.miningPeriod)

    private fun List<AutoInvestTask>.groupedByChainId(): Map<Long, List<AutoInvestTask>> =
        groupBy { it.chainId }

    private fun AutoInvestTask.toRecord() =
        InvestmentRecord(this.userWalletAddress, this.campaignContractAddress, this.amount)
}
