package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.persistence.model.BlockchainTask
import com.ampnet.identityservice.persistence.model.BlockchainTaskStatus
import com.ampnet.identityservice.persistence.repository.BlockchainTaskRepository
import com.ampnet.identityservice.service.ScheduledExecutorServiceProvider
import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import com.ampnet.identityservice.util.ChainId
import com.ampnet.identityservice.util.TransactionHash
import mu.KLogging
import org.springframework.beans.factory.DisposableBean
import java.util.concurrent.TimeUnit

abstract class AbstractBlockchainQueue(
    protected val uuidProvider: UuidProvider,
    protected val timeProvider: ZonedDateTimeProvider,
    protected val blockchainService: BlockchainService,
    protected val applicationProperties: ApplicationProperties,
    protected val blockchainTaskRepository: BlockchainTaskRepository,
    protected val pendingBlockchainTaskRepository: BlockchainTaskRepository,
    private val queueName: String,
    scheduledExecutorServiceProvider: ScheduledExecutorServiceProvider
) : DisposableBean, KLogging() {

    private val executorService = scheduledExecutorServiceProvider.newSingleThreadScheduledExecutor(queueName)

    init {
        executorService.scheduleAtFixedRate(
            { processTasks() },
            applicationProperties.queue.initialDelay,
            applicationProperties.queue.polling,
            TimeUnit.MILLISECONDS
        )
    }

    override fun destroy() {
        logger.info { "Shutting down $queueName executor service..." }
        executorService.shutdown()
    }

    protected abstract fun createBlockchainTaskFromPendingTask()

    protected abstract fun executeBlockchainTask(task: BlockchainTask): TransactionHash?

    @Suppress("TooGenericExceptionCaught")
    private fun processTasks() {
        // creates pending tasks for all chains which have at least one address in the queue
        createBlockchainTaskFromPendingTask()

        blockchainTaskRepository.getInProcess()?.let {
            try {
                handleInProcessTask(it)
            } catch (ex: Throwable) {
                logger.error("Failed to handle in process task: ${ex.message}")
                markTaskAsFailedAndRetryWithNewTask(it)
            }

            return
        }

        blockchainTaskRepository.getPending()?.let {
            try {
                handlePendingTask(it)
            } catch (ex: Throwable) {
                logger.error("Failed to handle pending task: ${ex.message}")
                markTaskAsFailedAndRetryWithNewTask(it)
            }

            return
        }
    }

    private fun handleInProcessTask(task: BlockchainTask) {
        logger.debug { "Task in process: $task" }
        if (task.hash == null) {
            logger.warn { "Task is process is missing hash: $task" }
        }

        task.hash?.let { hash ->
            if (blockchainService.isMined(TransactionHash(hash), ChainId(task.chainId))) {
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

    private fun handlePendingTask(task: BlockchainTask) {
        val hash = executeBlockchainTask(task) ?: return
        logger.info { "Handling process for addresses: ${task.addresses.contentToString()} with hash: $hash" }
        blockchainTaskRepository.setStatus(task.uuid, BlockchainTaskStatus.IN_PROCESS, hash.value, task.payload)
    }

    private fun handleMinedTransaction(task: BlockchainTask) {
        blockchainTaskRepository.setStatus(task.uuid, BlockchainTaskStatus.COMPLETED, task.hash, task.payload)
        logger.info { "Task is completed: $task" }
    }

    private fun markTaskAsFailedAndRetryWithNewTask(task: BlockchainTask) {
        blockchainTaskRepository.setStatus(task.uuid, BlockchainTaskStatus.FAILED, task.hash, task.payload)
        blockchainTaskRepository.saveAndFlush(
            BlockchainTask(
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
