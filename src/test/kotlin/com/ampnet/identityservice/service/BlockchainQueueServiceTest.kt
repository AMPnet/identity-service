package com.ampnet.identityservice.service

import com.ampnet.identityservice.controller.ControllerTestBase
import com.ampnet.identityservice.persistence.model.BlockchainTask
import com.ampnet.identityservice.persistence.model.BlockchainTaskStatus
import com.ampnet.identityservice.persistence.repository.BlockchainTaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime

class BlockchainQueueServiceTest : ControllerTestBase() {

    private val address = "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF7"
    private val hash = "0x6f7dea8d5d98d119de31204dfbdc69bb1944db04891ad0c45ab577da8e6de04a"
    private lateinit var testContext: TestContext

    @Autowired
    private lateinit var blockchainTaskRepository: BlockchainTaskRepository

    @BeforeEach
    fun init() {
        testContext = TestContext()
        databaseCleanerService.deleteAllBlockchainTasks()
    }

    @AfterEach
    fun after() {
        databaseCleanerService.deleteAllBlockchainTasks()
    }

    @Test
    fun mustHandleSingleTaskInQueue() {
        suppose("Blockchain service will whitelist address") {
            given(blockchainService.whitelistAddress(address)).willReturn(hash)
        }
        suppose("Transaction is mined") {
            given(blockchainService.isMined(hash)).willReturn(true)
        }
        suppose("There is a task in queue") {
            testContext.task = createBlockchainTask()
        }

        verify("Service will handle the task") {
            waitUntilTasksAreProcessed()
            val tasks = blockchainTaskRepository.findAll()
            assertThat(tasks).hasSize(1)
            assertThat(tasks.first().status).isEqualTo(BlockchainTaskStatus.COMPLETED)
        }
    }

    @Test
    fun mustHandleTwoNewTasksInQueue() {
        suppose("Blockchain service will whitelist any address") {
            given(blockchainService.whitelistAddress(any())).willReturn(hash)
        }
        suppose("Transactions are mined") {
            given(blockchainService.isMined(any())).willReturn(true)
        }
        suppose("There are two tasks in queue") {
            testContext.task = createBlockchainTask()
            createBlockchainTask()
        }

        verify("Service will handle the task") {
            waitUntilTasksAreProcessed()
            val tasks = blockchainTaskRepository.findAll()
            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED }
        }
    }

    @Test
    fun mustHandleTaskInProcessAndNewTask() {
        suppose("There is task in process") {
            testContext.task = createBlockchainTask(status = BlockchainTaskStatus.IN_PROCESS, hash = hash)
        }
        suppose("New task is created") {
            createBlockchainTask()
        }
        suppose("Blockchain service will whitelist any address") {
            given(blockchainService.whitelistAddress(any())).willReturn(hash)
        }
        suppose("Transactions are mined") {
            given(blockchainService.isMined(any())).willReturn(true)
        }

        verify("Both tasks are handled in correct order") {
            waitUntilTasksAreProcessed()
            val tasks = blockchainTaskRepository.findAll()
            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED }
            tasks.sortBy { it.createdAt }
            assertThat(tasks.first().uuid).isEqualTo(testContext.task.uuid)
            assertThat(tasks.first().updatedAt).isBefore(tasks[1].updatedAt)
        }
    }

    @Test
    fun mustHandleNotMinedTransaction() {
        suppose("There is task in process") {
            testContext.task = createBlockchainTask(
                status = BlockchainTaskStatus.IN_PROCESS,
                hash = hash,
                updatedAt = zonedDateTimeProvider.getZonedDateTime().minusMinutes(applicationProperties.queue.waiting + 1)
            )
        }
        suppose("Transaction is not mined") {
            given(blockchainService.isMined(any())).willReturn(false)
        }

        verify("Task will fail") {
            waitUntilTasksAreProcessed()
            val tasks = blockchainTaskRepository.findAll()
            assertThat(tasks).hasSize(1)
            assertThat(tasks.first().status).isEqualTo(BlockchainTaskStatus.FAILED)
        }
    }

    @Test
    fun mustStartTaskAfterFailedTask() {
        suppose("Transaction is not mined") {
            given(blockchainService.isMined(any())).willReturn(false)
        }
        suppose("There is task in process") {
            createBlockchainTask(
                payload = "some_address",
                status = BlockchainTaskStatus.IN_PROCESS,
                hash = "some_hash",
                updatedAt = zonedDateTimeProvider.getZonedDateTime().minusMinutes(applicationProperties.queue.waiting * 2)
            )
        }
        suppose("New task is created") {
            testContext.task = createBlockchainTask()
        }
        suppose("Blockchain service will mine transaction") {
            given(blockchainService.whitelistAddress(address)).willReturn(hash)
        }
        suppose("New transaction will be mined") {
            given(blockchainService.isMined(hash)).willReturn(true)
        }

        verify("First task failed") {
            waitUntilTasksAreProcessed()
            val task = blockchainTaskRepository.findAll().firstOrNull { it.uuid != testContext.task.uuid }
                ?: fail("Missing failed transaction")
            assertThat(task.status).isEqualTo(BlockchainTaskStatus.FAILED)
        }
        verify("Second task is completed") {
            waitUntilTasksAreProcessed()
            val task = blockchainTaskRepository.findById(testContext.task.uuid).unwrap()
            assertThat(task?.status).isEqualTo(BlockchainTaskStatus.COMPLETED)
        }
    }

    @Test
    fun mustHandleMultipleTasks() {
        suppose("Blockchain service will whitelist any address") {
            given(blockchainService.whitelistAddress(any())).willReturn(hash)
        }
        suppose("Transactions are mined") {
            given(blockchainService.isMined(any())).willReturn(true)
        }
        suppose("There are multiple tasks") {
            for (i in 1..10) {
                createBlockchainTask()
            }
        }

        verify("Tasks are handled") {
            waitUntilTasksAreProcessed()
            val tasks = blockchainTaskRepository.findAll()
            assertThat(tasks).hasSize(10)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED }
        }
    }

    private fun waitUntilTasksAreProcessed(retry: Int = 5) {
        Thread.sleep(applicationProperties.queue.initialDelay * 10)
        if (retry == 0) return
        blockchainTaskRepository.getFirstPending()?.let {
            Thread.sleep(applicationProperties.queue.polling * 2)
            waitUntilTasksAreProcessed(retry - 1)
        }
        blockchainTaskRepository.getInProcess()?.let {
            Thread.sleep(applicationProperties.queue.polling * 2)
            waitUntilTasksAreProcessed(retry - 1)
        }
    }

    private fun createBlockchainTask(
        status: BlockchainTaskStatus = BlockchainTaskStatus.CREATED,
        payload: String = address,
        hash: String? = null,
        updatedAt: ZonedDateTime? = null
    ): BlockchainTask {
        val task = BlockchainTask(
            uuidProvider.getUuid(),
            payload,
            status,
            hash,
            zonedDateTimeProvider.getZonedDateTime(),
            updatedAt
        )
        return blockchainTaskRepository.save(task)
    }

    private class TestContext {
        lateinit var task: BlockchainTask
    }
}
