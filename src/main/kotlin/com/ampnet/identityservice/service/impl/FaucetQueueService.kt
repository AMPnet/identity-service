package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.persistence.model.FaucetTask
import com.ampnet.identityservice.persistence.model.FaucetTaskStatus
import com.ampnet.identityservice.persistence.repository.FaucetTaskRepository
import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class FaucetQueueService(
    private val faucetTaskRepository: FaucetTaskRepository,
    private val uuidProvider: UuidProvider,
    private val timeProvider: ZonedDateTimeProvider,
    private val blockchainService: BlockchainService,
    private val applicationProperties: ApplicationProperties
) {

    companion object : KLogging()

    private val executorService = Executors.newSingleThreadScheduledExecutor()

    init {
        executorService.scheduleAtFixedRate(
            { processTasks() },
            applicationProperties.queue.initialDelay,
            applicationProperties.queue.polling,
            TimeUnit.MILLISECONDS
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processTasks() {
        // creates pending tasks for all chains which have at least one address in the queue
        faucetTaskRepository.fetchChainIdsWithPendingAddresses().forEach { chainId ->
            faucetTaskRepository.flushAddressQueueForChainId(
                uuidProvider.getUuid(),
                chainId,
                timeProvider.getZonedDateTime()
            )
        }

        faucetTaskRepository.getInProcess()?.let {
            try {
                handleInProcessTask(it)
            } catch (ex: Throwable) {
                logger.error("Failed to handle in process task: ${ex.message}")
                faucetTaskRepository.setStatus(it.uuid, FaucetTaskStatus.FAILED, it.hash)
            }

            return
        }

        faucetTaskRepository.getPending()?.let {
            try {
                handlePendingTask(it)
            } catch (ex: Throwable) {
                logger.error("Failed to handle pending task: ${ex.message}")
                faucetTaskRepository.setStatus(it.uuid, FaucetTaskStatus.FAILED, it.hash)
            }

            return
        }
    }

    private fun handleInProcessTask(task: FaucetTask) {
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
                    faucetTaskRepository.setStatus(task.uuid, FaucetTaskStatus.FAILED, task.hash)
                } else {
                    logger.info { "Waiting for task to be mined: $hash" }
                }
            }
        }
    }

    private fun handlePendingTask(task: FaucetTask) {
        logger.debug { "Starting to process task: $task" }
        val hash = blockchainService.sendFaucetFunds(task.addresses.toList(), task.chainId)

        if (hash.isNullOrEmpty()) {
            logger.warn { "Failed to get hash for faucet task: ${task.uuid}" }
            return
        }

        logger.info { "Sending faucet funds to addresses: ${task.addresses.contentToString()} with hash: $hash" }
        faucetTaskRepository.setStatus(task.uuid, FaucetTaskStatus.IN_PROCESS, hash)
    }

    private fun handleMinedTransaction(task: FaucetTask) {
        logger.info { "Transaction is mined: ${task.hash}" }
        faucetTaskRepository.setStatus(task.uuid, FaucetTaskStatus.COMPLETED, task.hash)
        logger.info {
            "Faucet funds sent to addresses: ${task.addresses.contentToString()}. Task is completed: ${task.hash}"
        }
    }

    private fun getMaximumMiningPeriod() = timeProvider.getZonedDateTime()
        .minusSeconds(applicationProperties.queue.miningPeriod)
}
