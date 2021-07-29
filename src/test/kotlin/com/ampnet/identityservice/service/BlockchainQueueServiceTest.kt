package com.ampnet.identityservice.service

import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.blockchain.BlockchainService
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
    private val issuerAddress = "0xb070a65b1dd7f49c90a59000bd8cca3259064d81"
    private lateinit var testContext: TestContext

    @Autowired
    private lateinit var databaseCleanerService: DatabaseCleanerService

    @Autowired
    private lateinit var blockchainTaskRepository: BlockchainTaskRepository

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    @Autowired
    private lateinit var zonedDateTimeProvider: ZonedDateTimeProvider

    @Autowired
    private lateinit var uuidProvider: UuidProvider

    @Autowired
    private lateinit var queueService: BlockchainQueueService

    @MockBean
    private lateinit var blockchainService: BlockchainService

    @BeforeEach
    fun init() {
        testContext = TestContext()
        databaseCleanerService.deleteAllBlockchainTasks()
        given(blockchainService.isWhitelisted(any(), any())).willReturn(true)
    }

    @AfterEach
    fun after() {
        databaseCleanerService.deleteAllBlockchainTasks()
    }

    @Test
    fun mustHandleSingleTaskInQueue() {
        suppose("Blockchain service will whitelist address") {
            given(blockchainService.whitelistAddress(address, issuerAddress)).willReturn(hash)
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
            given(blockchainService.whitelistAddress(any(), any())).willReturn(hash)
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
            given(blockchainService.whitelistAddress(any(), any())).willReturn(hash)
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
                updatedAt = zonedDateTimeProvider.getZonedDateTime()
                    .minusMinutes(applicationProperties.queue.miningPeriod * 2)
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
                updatedAt = zonedDateTimeProvider.getZonedDateTime()
                    .minusMinutes(applicationProperties.queue.miningPeriod * 2)
            )
        }
        suppose("Blockchain service will mine transaction") {
            given(blockchainService.whitelistAddress(address, issuerAddress)).willReturn(hash)
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
            given(blockchainService.whitelistAddress(any(), any())).willReturn(hash)
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

    @Test
    fun mustHandleMinedButNotWhitelistedAddress() {
        suppose("Blockchain service will whitelist address") {
            given(blockchainService.whitelistAddress(address, issuerAddress)).willReturn(hash)
        }
        suppose("Transaction is mined") {
            given(blockchainService.isMined(hash)).willReturn(true)
        }
        suppose("Blockchain service did not managed to whitelist address") {
            given(blockchainService.isWhitelisted(address, issuerAddress)).willReturn(false)
        }
        suppose("There is a task in queue") {
            testContext.task = createBlockchainTask()
        }

        verify("Service will handle the task") {
            waitUntilTasksAreProcessed()
            val tasks = blockchainTaskRepository.findAll()
            assertThat(tasks).hasSize(1)
            assertThat(tasks.first().status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(tasks.first().hash).isEqualTo(hash)
        }
    }

    @Test
    fun mustNotCreateNewTaskForWhitelistedAddress() {
        suppose("There is whitelisted address for issuer") {
            createBlockchainTask(BlockchainTaskStatus.COMPLETED, hash = hash)
        }
        suppose("Whitelisting request is received") {
            queueService.createWhitelistAddressTask(address, issuerAddress)
        }

        verify("Service will not create a new task") {
            waitUntilTasksAreProcessed()
            val tasks = blockchainTaskRepository.findAll()
            assertThat(tasks).hasSize(1)
            assertThat(tasks.first().status).isEqualTo(BlockchainTaskStatus.COMPLETED)
            assertThat(tasks.first().hash).isEqualTo(hash)
        }
    }

    @Test
    fun mustCreateNewTaskForFailedWhitelistedAddress() {
        suppose("There is whitelisted address for issuer") {
            createBlockchainTask(BlockchainTaskStatus.FAILED)
        }
        suppose("Blockchain service will mine transaction") {
            given(blockchainService.whitelistAddress(address, issuerAddress)).willReturn(hash)
        }
        suppose("New transaction will be mined") {
            given(blockchainService.isMined(hash)).willReturn(true)
        }
        suppose("Whitelisting request is received") {
            queueService.createWhitelistAddressTask(address, issuerAddress)
        }

        verify("Service created new task") {
            waitUntilTasksAreProcessed()
            val tasks = blockchainTaskRepository.findAll()
            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.payload == address }
            assertThat(tasks.map { it.status })
                .containsAll(listOf(BlockchainTaskStatus.FAILED, BlockchainTaskStatus.COMPLETED))
        }
    }

    @Test
    fun mustCreateNewTaskForWhitelistedWalletForNewIssuer() {
        suppose("There is whitelisted address for issuer") {
            createBlockchainTask(BlockchainTaskStatus.COMPLETED, hash = hash)
        }
        suppose("There is request for whitelisting the address for new issuer") {
            createBlockchainTask(BlockchainTaskStatus.CREATED, contractAddress = "new_issuer_address")
        }
        suppose("Blockchain service will mine transaction") {
            given(blockchainService.whitelistAddress(address, "new_issuer_address")).willReturn(hash)
        }
        suppose("New transaction will be mined") {
            given(blockchainService.isMined(hash)).willReturn(true)
        }

        verify("New task is handled") {
            waitUntilTasksAreProcessed()
            val tasks = blockchainTaskRepository.findAll()
            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED && it.payload == address }
            assertThat(tasks.map { it.contractAddress }).containsAll(listOf(issuerAddress, "new_issuer_address"))
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
        contractAddress: String = issuerAddress,
        hash: String? = null,
        updatedAt: ZonedDateTime? = null
    ): BlockchainTask {
        val task = BlockchainTask(
            uuidProvider.getUuid(),
            payload,
            contractAddress,
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
