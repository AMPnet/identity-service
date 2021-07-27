package com.ampnet.identityservice.service

import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.config.DatabaseCleanerService
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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.ZonedDateTime

@SpringBootTest
class BlockchainQueueServiceTest : TestBase() {

    private val address = "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF7"
    private val hash = "0x6f7dea8d5d98d119de31204dfbdc69bb1944db04891ad0c45ab577da8e6de04a"
    private lateinit var testContext: TestContext

    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService

    @Autowired
    private lateinit var blockchainTaskRepository: BlockchainTaskRepository

    @Autowired
    protected lateinit var applicationProperties: ApplicationProperties

    @Autowired
    protected lateinit var zonedDateTimeProvider: ZonedDateTimeProvider

    @Autowired
    protected lateinit var uuidProvider: UuidProvider

    @MockBean
    protected lateinit var blockchainService: BlockchainService

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
            assertThat(tasks.first().hash).isEqualTo(hash)
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
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED && it.hash == hash }
        }
    }

    @Test
    fun mustHandleTaskInProcessAndNewTask() {
        suppose("Blockchain service will whitelist any address") {
            given(blockchainService.whitelistAddress(any())).willReturn(hash)
        }
        suppose("Transactions are mined") {
            given(blockchainService.isMined(any())).willReturn(true)
        }
        suppose("There is task in process") {
            testContext.task = createBlockchainTask(status = BlockchainTaskStatus.IN_PROCESS, hash = hash)
        }
        suppose("New task is created") {
            createBlockchainTask()
        }

        verify("Both tasks are handled in correct order") {
            waitUntilTasksAreProcessed()
            val tasks = blockchainTaskRepository.findAll()
            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED && it.hash == hash }
            tasks.sortBy { it.createdAt }
            assertThat(tasks.first().uuid).isEqualTo(testContext.task.uuid)
            assertThat(tasks.first().updatedAt).isBefore(tasks[1].updatedAt)
        }
    }

    @Test
    fun mustHandleNotMinedTransaction() {
        suppose("Transaction is not mined") {
            given(blockchainService.isMined(any())).willReturn(false)
        }
        suppose("There is task in process") {
            testContext.task = createBlockchainTask(
                status = BlockchainTaskStatus.IN_PROCESS,
                hash = hash,
                updatedAt = zonedDateTimeProvider.getZonedDateTime().minusMinutes(applicationProperties.queue.waiting + 1)
            )
        }

        verify("Task will fail") {
            waitUntilTasksAreProcessed()
            val tasks = blockchainTaskRepository.findAll()
            assertThat(tasks).hasSize(1)
            assertThat(tasks.first().status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(tasks.first().hash).isEqualTo(hash)
        }
    }

    @Test
    fun mustStartTaskAfterFailedTask() {
        suppose("Transaction is not mined") {
            given(blockchainService.isMined("some_hash")).willReturn(false)
        }
        suppose("There is task in process") {
            createBlockchainTask(
                payload = "some_address",
                status = BlockchainTaskStatus.IN_PROCESS,
                hash = "some_hash",
                updatedAt = zonedDateTimeProvider.getZonedDateTime().minusMinutes(applicationProperties.queue.waiting * 2)
            )
        }
        suppose("Blockchain service will mine transaction") {
            given(blockchainService.whitelistAddress(address)).willReturn(hash)
        }
        suppose("New transaction will be mined") {
            given(blockchainService.isMined(hash)).willReturn(true)
        }
        suppose("New task is created") {
            testContext.task = createBlockchainTask()
        }

        verify("First task failed") {
            waitUntilTasksAreProcessed()
            val task = blockchainTaskRepository.findAll().firstOrNull { it.uuid != testContext.task.uuid }
                ?: fail("Missing failed transaction")
            assertThat(task.status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(task.hash).isEqualTo("some_hash")
        }
        verify("Second task is completed") {
            waitUntilTasksAreProcessed()
            val task = blockchainTaskRepository.findById(testContext.task.uuid).unwrap()
                ?: fail("Missing transaction")
            assertThat(task.status).isEqualTo(BlockchainTaskStatus.COMPLETED)
            assertThat(task.hash).isEqualTo(hash)
        }
    }

    @Test
    fun mustHandleMultipleTasksInOrder() {
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

        verify("Tasks are handled in order") {
            waitUntilTasksAreProcessed()
            val tasks = blockchainTaskRepository.findAll()
            tasks.sortBy { it.createdAt }
            assertThat(tasks).hasSize(10)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED && it.hash == hash }
            for (i in 0..8) {
                assertThat(tasks[i].createdAt).isBefore(tasks[i + 1].createdAt)
                assertThat(tasks[i].updatedAt).isBefore(tasks[i + 1].updatedAt)
            }
        }
    }

    private fun waitUntilTasksAreProcessed(retry: Int = 5) {
        if (retry == 0) return
        Thread.sleep(applicationProperties.queue.initialDelay * 2)
        blockchainTaskRepository.getPending()?.let {
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
