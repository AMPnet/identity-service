package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.controller.pojo.request.WhitelistRequest
import com.ampnet.identityservice.persistence.model.BlockchainTask
import com.ampnet.identityservice.persistence.repository.BlockchainTaskRepository
import com.ampnet.identityservice.service.ScheduledExecutorServiceProvider
import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.WhitelistQueueService
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
) : WhitelistQueueService, AbstractBlockchainQueue(
    uuidProvider,
    timeProvider,
    blockchainService,
    applicationProperties,
    blockchainTaskRepository,
    pendingBlockchainTaskRepository,
    "WhitelistQueue",
    scheduledExecutorServiceProvider
) {

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

    override fun executeBlockchainTask(task: BlockchainTask): String? =
        task.payload?.let {
            val hash = blockchainService.whitelistAddresses(task.addresses.toList(), it, task.chainId)
            if (hash == null) {
                logger.warn { "Failed to whitelist addresses for task: ${task.uuid}" }
            }
            hash
        }

    override fun addAddressToQueue(address: String, request: WhitelistRequest) {
        if (blockchainService.isWhitelisted(address, request.issuerAddress, request.chainId)) {
            logger.info { "Address: $address for request: $request already whitelisted " }
            return
        }
        logger.info { "Adding address: $address for whitelisting" }
        pendingBlockchainTaskRepository.addAddressToQueue(address, request.chainId, request.issuerAddress)
        logger.debug { "Created BlockchainTask" }
    }
}
