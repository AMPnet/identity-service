package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.persistence.model.BlockchainTask
import com.ampnet.identityservice.persistence.repository.BlockchainTaskRepository
import com.ampnet.identityservice.service.ScheduledExecutorServiceProvider
import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import com.ampnet.identityservice.util.ChainId
import com.ampnet.identityservice.util.TransactionHash
import com.ampnet.identityservice.util.WalletAddress
import org.springframework.stereotype.Service

@Service
class FaucetQueueService(
    uuidProvider: UuidProvider,
    timeProvider: ZonedDateTimeProvider,
    blockchainService: BlockchainService,
    applicationProperties: ApplicationProperties,
    blockchainTaskRepository: BlockchainTaskRepository,
    pendingBlockchainTaskRepository: BlockchainTaskRepository,
    scheduledExecutorServiceProvider: ScheduledExecutorServiceProvider
) : AbstractBlockchainQueue(
    uuidProvider,
    timeProvider,
    blockchainService,
    applicationProperties,
    blockchainTaskRepository,
    pendingBlockchainTaskRepository,
    "FaucetQueue",
    scheduledExecutorServiceProvider
) {

    override fun createBlockchainTaskFromPendingTask() {
        blockchainTaskRepository.fetchChainIdsWithPendingAddresses().forEach { chainId ->
            blockchainTaskRepository.flushAddressQueueForChainId(
                uuidProvider.getUuid(),
                chainId,
                timeProvider.getZonedDateTime(),
                applicationProperties.queue.maxAddressesPerTask
            )
        }
    }

    override fun executeBlockchainTask(task: BlockchainTask): TransactionHash? =
        if (task.payload == null) {
            val hash = blockchainService.sendFaucetFunds(
                task.addresses.toList().map { WalletAddress(it) },
                ChainId(task.chainId)
            )
            if (hash == null) {
                logger.warn { "Failed to send faucet funds for task: ${task.uuid}" }
            }
            hash
        } else {
            null
        }
}
