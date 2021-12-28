package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.controller.pojo.request.WhitelistRequest
import com.ampnet.identityservice.persistence.model.FaucetTask
import com.ampnet.identityservice.persistence.model.FaucetTaskStatus
import com.ampnet.identityservice.persistence.repository.FaucetTaskRepository
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
    private val uuidProvider: UuidProvider,
    private val timeProvider: ZonedDateTimeProvider,
    private val blockchainService: BlockchainService,
    private val applicationProperties: ApplicationProperties,
    private val faucetTaskRepository: FaucetTaskRepository,
    private val pendingFaucetTaskRepository: FaucetTaskRepository,
    scheduledExecutorServiceProvider: ScheduledExecutorServiceProvider
) : BlockchainQueueService, DisposableBean {

    companion object : KLogging() {
        const val QUEUE_NAME = "WhitelistQueue"
    }

    private val executorService = scheduledExecutorServiceProvider.newSingleThreadScheduledExecutor(QUEUE_NAME)

    init {
        executorService.scheduleAtFixedRate(
            { processTasks() },
            applicationProperties.queue.initialDelay - applicationProperties.queue.polling.div(2),
            applicationProperties.queue.polling,
            TimeUnit.MILLISECONDS
        )
    }

    override fun destroy() {
        logger.info { "Shutting down blockchain queue executor service..." }
        executorService.shutdown()
    }

    override fun createWhitelistAddressTask(address: String, request: WhitelistRequest) {
        if (blockchainService.isWhitelisted(address, request.issuerAddress, request.chainId)) {
            logger.info { "Address: $address for request: $request already whitelisted or in process" }
            return
        }
        logger.info { "Received task for address: $address" }
        pendingFaucetTaskRepository.addAddressToQueue(address, request.chainId, request.issuerAddress)
//        val blockchainTask = BlockchainTask(address, request.issuerAddress, request.chainId, uuidProvider, timeProvider)
//        blockchainTaskRepository.save(blockchainTask)
        logger.info { "Created BlockchainTask" }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processTasks() {
        // creates pending tasks for all chains which have at least one address in the queue
        faucetTaskRepository.fetchChainIdsWithPendingAddresses().forEach { chainId ->
            faucetTaskRepository.fetchPayloadsWithPendingAddresses().forEach { issuerAddress ->
                faucetTaskRepository.flushAddressQueueForChainId(
                    uuidProvider.getUuid(),
                    chainId,
                    timeProvider.getZonedDateTime(),
                    applicationProperties.faucet.maxAddressesPerTask,
                    issuerAddress
                )
            }
        }

        faucetTaskRepository.getInProcess()?.let {
            try {
                handleInProcessTask(it)
            } catch (ex: Throwable) {
                logger.error("Failed to handle in process task: ${ex.message}")
                markTaskAsFailedAndRetryWithNewTask(it)
            }

            return
        }

        faucetTaskRepository.getPending()?.let {
            try {
                handlePendingTask(it)
            } catch (ex: Throwable) {
                logger.error("Failed to handle pending task: ${ex.message}")
                markTaskAsFailedAndRetryWithNewTask(it)
            }

            return
        }
    }

    private fun handleInProcessTask(task: FaucetTask) {
        logger.debug { "Task in process: $task" }
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
                    markTaskAsFailedAndRetryWithNewTask(task)
                } else {
                    logger.info { "Waiting for task to be mined: $hash" }
                }
            }
        }
    }

    private fun handlePendingTask(task: FaucetTask) {
        logger.debug { "Starting to process task: $task" }
        val issuerAddress = task.payload ?: return
        val hash = blockchainService.whitelistAddress(task.addresses.toList(), issuerAddress, task.chainId)

        if (hash.isNullOrEmpty()) {
            logger.warn { "Failed to get hash for faucet task: ${task.uuid}" }
            return
        }

        logger.info {
            "Whitelisting for issuer: $issuerAddress for wallets: " +
                "${task.addresses.contentToString()} with hash: $hash"
        }
        faucetTaskRepository.setStatus(task.uuid, FaucetTaskStatus.IN_PROCESS, hash, issuerAddress)
    }

    private fun handleMinedTransaction(task: FaucetTask) {
        logger.info { "Transaction is mined: ${task.hash}" }
        faucetTaskRepository.setStatus(task.uuid, FaucetTaskStatus.COMPLETED, task.hash, task.payload)
        logger.info {
            "Faucet funds sent to addresses: ${task.addresses.contentToString()}. Task is completed: ${task.hash}"
        }
    }

    private fun markTaskAsFailedAndRetryWithNewTask(task: FaucetTask) {
        faucetTaskRepository.setStatus(task.uuid, FaucetTaskStatus.FAILED, task.hash, task.payload)
        faucetTaskRepository.saveAndFlush(
            FaucetTask(
                task.addresses,
                task.chainId,
                uuidProvider,
                timeProvider,
                task.payload
            )
        )
    }

    private fun getMaximumMiningPeriod() = timeProvider.getZonedDateTime()
        .minusSeconds(applicationProperties.queue.miningPeriod)
}
