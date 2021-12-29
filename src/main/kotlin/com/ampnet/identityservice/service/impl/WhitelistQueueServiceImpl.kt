package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.controller.pojo.request.WhitelistRequest
import com.ampnet.identityservice.persistence.repository.BlockchainTaskRepository
import com.ampnet.identityservice.service.BlockchainQueueService
import com.ampnet.identityservice.service.ScheduledExecutorServiceProvider
import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import org.springframework.stereotype.Service

@Service
class WhitelistQueueServiceImpl(
    uuidProvider: UuidProvider,
    timeProvider: ZonedDateTimeProvider,
    blockchainService: BlockchainService,
    applicationProperties: ApplicationProperties,
    blockchainTaskRepository: BlockchainTaskRepository,
    pendingBlockchainTaskRepository: BlockchainTaskRepository,
    scheduledExecutorServiceProvider: ScheduledExecutorServiceProvider
) : BlockchainQueueService, AbstractBlockchainQueue(
    uuidProvider,
    timeProvider,
    blockchainService,
    applicationProperties,
    blockchainTaskRepository,
    pendingBlockchainTaskRepository,
    scheduledExecutorServiceProvider
) {

    companion object {
        const val QUEUE_NAME = "WhitelistQueue"
    }

    override val queueName: String
        get() = QUEUE_NAME

    override fun createBlockchainTaskFromPendingTask() {
        blockchainTaskRepository.fetchChainIdsWithPendingAddresses().forEach { chainId ->
            blockchainTaskRepository.fetchPayloadsWithPendingAddresses().forEach { issuerAddress ->
                blockchainTaskRepository.flushAddressQueueForChainId(
                    uuidProvider.getUuid(),
                    chainId,
                    timeProvider.getZonedDateTime(),
                    applicationProperties.faucet.maxAddressesPerTask,
                    issuerAddress
                )
            }
        }
    }

    override fun createWhitelistAddressTask(address: String, request: WhitelistRequest) {
        if (blockchainService.isWhitelisted(address, request.issuerAddress, request.chainId)) {
            logger.info { "Address: $address for request: $request already whitelisted or in process" }
            return
        }
        logger.info { "Received task for address: $address" }
        pendingBlockchainTaskRepository.addAddressToQueue(address, request.chainId, request.issuerAddress)
        logger.info { "Created BlockchainTask" }
    }
}
