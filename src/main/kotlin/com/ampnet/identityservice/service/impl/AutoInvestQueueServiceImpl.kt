package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.controller.pojo.request.AutoInvestRequest
import com.ampnet.identityservice.controller.pojo.response.AutoInvestResponse
import com.ampnet.identityservice.persistence.model.AutoInvestTask
import com.ampnet.identityservice.persistence.model.AutoInvestTaskStatus
import com.ampnet.identityservice.persistence.repository.AutoInvestTaskRepository
import com.ampnet.identityservice.service.AutoInvestQueueService
import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class AutoInvestQueueServiceImpl(
    private val autoInvestTaskRepository: AutoInvestTaskRepository,
    private val uuidProvider: UuidProvider,
    private val timeProvider: ZonedDateTimeProvider,
    private val blockchainService: BlockchainService,
    applicationProperties: ApplicationProperties
) : AutoInvestQueueService {

    companion object : KLogging()

    private val executorService = Executors.newSingleThreadScheduledExecutor()

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

    override fun createOrUpdateAutoInvestTask(
        address: String,
        chainId: Long,
        request: AutoInvestRequest
    ): AutoInvestResponse? {
        val numModified = autoInvestTaskRepository.createOrUpdate(
            AutoInvestTask(
                chainId = chainId,
                userWalletAddress = address,
                campaignContractAddress = request.campaignAddress,
                amount = request.amount,
                status = AutoInvestTaskStatus.PENDING,
                uuidProvider = uuidProvider,
                timeProvider = timeProvider
            )
        )

        return if (numModified == 0) {
            logger.warn {
                "Auto-invest already in process for address: $address, campaign: $${request.campaignAddress}," +
                    " chainId: $chainId"
            }
            null
        } else {
            val task = autoInvestTaskRepository.findByUserWalletAddressAndCampaignContractAddressAndChainId(
                userWalletAddress = address,
                campaignContractAddress = request.campaignAddress,
                chainId = chainId
            )
            logger.info { "Submitted auto-invest task: $task" }
            AutoInvestResponse(
                walletAddress = task.userWalletAddress,
                campaignAddress = task.campaignContractAddress,
                amount = task.amount
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processTasks() {
        // TODO implement task processing
    }
}
