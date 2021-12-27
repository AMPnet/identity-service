package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.controller.pojo.request.WhitelistRequest
import com.ampnet.identityservice.persistence.model.BlockchainTask
import com.ampnet.identityservice.persistence.model.BlockchainTaskStatus
import com.ampnet.identityservice.persistence.repository.BlockchainTaskRepository
import com.ampnet.identityservice.service.BlockchainQueueService
import com.ampnet.identityservice.service.ScheduledExecutorServiceProvider
import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import mu.KLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class BlockchainQueueServiceImpl(
    private val blockchainTaskRepository: BlockchainTaskRepository,
    private val uuidProvider: UuidProvider,
    private val timeProvider: ZonedDateTimeProvider,
    private val blockchainService: BlockchainService,
    private val applicationProperties: ApplicationProperties,
    scheduledExecutorServiceProvider: ScheduledExecutorServiceProvider
) : BlockchainQueueService, DisposableBean {

    companion object : KLogging() {
        const val QUEUE_NAME = "WhitelistQueue"
    }

    private val executorService = scheduledExecutorServiceProvider.newSingleThreadScheduledExecutor(QUEUE_NAME)

    init {
        executorService.scheduleAtFixedRate(
            { processTasks() },
            applicationProperties.queue.initialDelay,
            applicationProperties.queue.polling,
            TimeUnit.MILLISECONDS
        )
    }

    override fun destroy() {
        logger.info { "Shutting down blockchain queue executor service..." }
        executorService.shutdown()
    }

    override fun createWhitelistAddressTask(address: String, request: WhitelistRequest) {
        if (isWhitelistedOrInProcess(address, request)) {
            logger.info { "Address: $address for request: $request already whitelisted or in process" }
            return
        }
        logger.info { "Received task for address: $address" }
        val blockchainTask = BlockchainTask(address, request.issuerAddress, request.chainId, uuidProvider, timeProvider)
        blockchainTaskRepository.save(blockchainTask)
        logger.info { "Created BlockchainTask: $blockchainTask" }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processTasks() {
        blockchainTaskRepository.getInProcess()?.let {
            try {
                handleInProcessTask(it)
            } catch (ex: Throwable) {
                logger.error("Failed to handle in process task: ${ex.message}")
                blockchainTaskRepository.setStatus(it.uuid, BlockchainTaskStatus.FAILED, it.hash)
            }
            return
        }
        blockchainTaskRepository.getPending()?.let {
            try {
                handlePendingTask(it)
            } catch (ex: Throwable) {
                logger.error("Failed to handle pending task: ${ex.message}")
                blockchainTaskRepository.setStatus(it.uuid, BlockchainTaskStatus.FAILED, it.hash)
            }
            return
        }
    }

    private fun handleInProcessTask(task: BlockchainTask) {
        logger.debug { "Task in process: $task " }
        if (task.hash == null) {
            logger.warn { "Task is process is missing hash: $task" }
        }
        task.hash?.let { hash ->
            if (blockchainService.isMined(hash, task.chainId)) {
                handleMinedTransaction(task)
            } else {
                if (task.updatedAt?.isBefore(getMaximumMiningPeriod()) == true) {
                    logger.warn {
                        "Waiting for transaction: $hash exceeded: ${applicationProperties.queue.miningPeriod} minutes"
                    }
                    blockchainTaskRepository.setStatus(task.uuid, BlockchainTaskStatus.FAILED, task.hash)
                } else {
                    logger.info { "Waiting for task to be mined: $hash" }
                }
            }
        }
    }

    private fun handlePendingTask(task: BlockchainTask) {
        logger.debug { "Starting to process task: $task" }
        val addresses = listOf(task.payload)
        val hash = blockchainService.whitelistAddress(addresses, task.contractAddress, task.chainId)
        if (hash.isNullOrEmpty()) {
            logger.warn { "Failed to get hash for whitelisting addresses: $addresses" }
            return
        }
        logger.info { "Whitelisting addresses: $addresses with hash: $hash" }
        blockchainTaskRepository.setStatus(task.uuid, BlockchainTaskStatus.IN_PROCESS, hash)
    }

    private fun handleMinedTransaction(task: BlockchainTask) {
        logger.info { "Transaction is mined: ${task.hash}" }
        if (blockchainService.isWhitelisted(task.payload, task.contractAddress, task.chainId)) {
            blockchainTaskRepository.setStatus(task.uuid, BlockchainTaskStatus.COMPLETED, task.hash)
            logger.info { "Address ${task.payload} is whitelisted. Task is completed: ${task.hash}" }
        } else {
            blockchainTaskRepository.setStatus(task.uuid, BlockchainTaskStatus.FAILED, task.hash)
            logger.error { "Address: ${task.payload} is not whitelisted. Transaction is failed: ${task.hash}" }
        }
    }

    private fun getMaximumMiningPeriod() = timeProvider.getZonedDateTime()
        .minusSeconds(applicationProperties.queue.miningPeriod)

    private fun isWhitelistedOrInProcess(address: String, request: WhitelistRequest): Boolean =
        blockchainTaskRepository.findByPayloadAndChainIdAndContractAddress(
            address, request.chainId, request.issuerAddress
        ).any { it.status != BlockchainTaskStatus.FAILED }
}
