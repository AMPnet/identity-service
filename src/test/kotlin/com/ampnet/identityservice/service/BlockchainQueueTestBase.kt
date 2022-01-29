package com.ampnet.identityservice.service

import com.ampnet.identityservice.ManualFixedScheduler
import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.blockchain.properties.Chain
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.config.DatabaseCleanerService
import com.ampnet.identityservice.config.TestSchedulerConfiguration
import com.ampnet.identityservice.persistence.model.BlockchainTask
import com.ampnet.identityservice.persistence.model.BlockchainTaskStatus
import com.ampnet.identityservice.persistence.repository.BlockchainTaskRepository
import com.ampnet.identityservice.util.ChainId
import com.ampnet.identityservice.util.TransactionHash
import com.ampnet.identityservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import java.time.ZonedDateTime

@SpringBootTest
@Import(TestSchedulerConfiguration::class)
abstract class BlockchainQueueTestBase : TestBase() {

    protected abstract val payload: String?
    protected val hash = TransactionHash("0x6f7dea8d5d98d119de31204dfbdc69bb1944db04891ad0c45ab577da8e6de04a")
    protected val addresses = listOf(
        WalletAddress("0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF7"),
        WalletAddress("0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF8"),
        WalletAddress("0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF9")
    )
    protected val chainId = Chain.MATIC_TESTNET_MUMBAI.id

    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService

    @Autowired
    protected lateinit var blockchainTaskRepository: BlockchainTaskRepository

    @Autowired
    protected lateinit var applicationProperties: ApplicationProperties

    @Autowired
    protected lateinit var zonedDateTimeProvider: ZonedDateTimeProvider

    @Autowired
    protected lateinit var uuidProvider: UuidProvider

    @MockBean
    protected lateinit var blockchainService: BlockchainService

    abstract var queueScheduler: ManualFixedScheduler

    abstract fun createTask(
        status: BlockchainTaskStatus = BlockchainTaskStatus.CREATED,
        addresses: List<WalletAddress> = this.addresses,
        chain: ChainId = chainId,
        hash: TransactionHash? = null,
        updatedAt: ZonedDateTime? = null
    ): BlockchainTask

    abstract fun addAddressToQueue(address: String, chainId: Long)

    abstract fun mockBlockchainTaskSuccessfulResponse()

    abstract fun mockBlockchainTaskExceptionResponse()

    abstract fun mockBlockchainHashNullResponse()

    @BeforeEach
    fun beforeEach() {
        databaseCleanerService.deleteAllBlockchainTasks()
        databaseCleanerService.deleteAllQueuedBlockchainAddresses()
    }

    @AfterEach
    fun afterEach() {
        databaseCleanerService.deleteAllBlockchainTasks()
        databaseCleanerService.deleteAllQueuedBlockchainAddresses()
    }

    @Test
    fun mustCreateNewTasksFromAddressQueue() {
        suppose("Blockchain service will successfully post requested task") {
            mockBlockchainTaskSuccessfulResponse()
        }

        suppose("Transactions are mined") {
            given(blockchainService.isMined(anyValueClass(TransactionHash("")), anyValueClass(ChainId(0L))))
                .willReturn(true)
        }

        suppose("There are some addresses in the queue for different chains") {
            addresses.forEach { addAddressToQueue(it.value, 1L) }
            addresses.forEach { addAddressToQueue(it.value, 2L) }
        }

        verify("Service will handle tasks created from address queue") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch {
                it.status == BlockchainTaskStatus.COMPLETED &&
                    it.hash == hash.value &&
                    it.payload == payload
            }
        }
    }

    @Test
    fun mustHandleSingleTaskInQueue() {
        suppose("Blockchain service will successfully post requested task") {
            mockBlockchainTaskSuccessfulResponse()
        }

        suppose("Transaction is mined") {
            given(blockchainService.isMined(anyValueClass(TransactionHash("")), anyValueClass(ChainId(0L))))
                .willReturn(true)
        }

        suppose("There is a task in queue") {
            createTask()
        }

        verify("Service will handle the task") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(1)
            assertThat(tasks).allMatch {
                it.status == BlockchainTaskStatus.COMPLETED &&
                    it.hash == hash.value &&
                    it.payload == payload
            }
        }
    }

    @Test
    fun mustRetryPendingTaskWhenExceptionIsThrown() {
        suppose("Blockchain service will throw exception") {
            mockBlockchainTaskExceptionResponse()
        }

        suppose("There is a task in queue") {
            createTask(status = BlockchainTaskStatus.CREATED)
        }

        verify("Task will fail and new task will be created") {
            processAllTasks(maxAttempts = 1)

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(tasks[1].status).isEqualTo(BlockchainTaskStatus.CREATED)
        }
    }

    @Test
    fun mustRetryInProcessTaskWhenExceptionIsThrown() {
        suppose("Blockchain service will throw exception") {
            given(blockchainService.isMined(hash.mockito(), chainId.mockito())).willThrow(RuntimeException())
        }

        suppose("There is a task in process") {
            createTask(status = BlockchainTaskStatus.IN_PROCESS, hash = hash)
        }

        verify("Task will fail and new task will be created") {
            processAllTasks(maxAttempts = 1)

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(tasks[0].hash).isEqualTo(hash.value)
            assertThat(tasks[0].payload).isEqualTo(payload)
            assertThat(tasks[1].status).isEqualTo(BlockchainTaskStatus.CREATED)
            assertThat(tasks[1].payload).isEqualTo(payload)
        }
    }

    @Test
    fun mustHandleSingleTaskInQueueWhenFirstReturnedHashIsNull() {
        suppose("Blockchain service will not successfully handle transaction") {
            mockBlockchainHashNullResponse()
        }

        suppose("There is a task in queue") {
            createTask()
        }

        verify("Task will remain in queue") {
            processAllTasks(maxAttempts = 10)

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(1)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.CREATED)
            assertThat(tasks[0].hash).isNull()
            assertThat(tasks[0].payload).isEqualTo(payload)
        }

        suppose("Blockchain service will successfully handle transaction") {
            mockBlockchainTaskSuccessfulResponse()
        }

        suppose("Transaction is mined") {
            given(blockchainService.isMined(anyValueClass(TransactionHash("")), anyValueClass(ChainId(0L))))
                .willReturn(true)
        }

        verify("Service will handle the task") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(1)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.COMPLETED)
            assertThat(tasks[0].hash).isEqualTo(hash.value)
            assertThat(tasks[0].payload).isEqualTo(payload)
        }
    }

    @Test
    fun mustHandleTwoNewTasksInQueue() {
        suppose("Blockchain service will successfully handle transaction for any address") {
            mockBlockchainTaskSuccessfulResponse()
        }

        suppose("Transactions are mined") {
            given(blockchainService.isMined(anyValueClass(TransactionHash("")), anyValueClass(ChainId(0L))))
                .willReturn(true)
        }

        suppose("There are two tasks in queue") {
            createTask()
            createTask()
        }

        verify("Service will handle the task") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED && it.hash == hash.value }
        }
    }

    @Test
    fun mustHandleInProcessTaskWhichExceededMiningPeriod() {
        suppose("There is a task in process which exceeds mining period") {
            createTask(
                status = BlockchainTaskStatus.IN_PROCESS,
                hash = hash,
                updatedAt = zonedDateTimeProvider.getZonedDateTime()
                    .minusSeconds(applicationProperties.queue.miningPeriod * 2)
            )
        }

        verify("Task will fail and new task will be created") {
            processAllTasks(maxAttempts = 1)

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(tasks[0].hash).isEqualTo(hash.value)
            assertThat(tasks[0].payload).isEqualTo(payload)
            assertThat(tasks[1].status).isEqualTo(BlockchainTaskStatus.CREATED)
            assertThat(tasks[1].payload).isEqualTo(payload)
        }
    }

    @Test
    fun mustHandleInProcessWhichIsWaitingForTransactionToBeMined() {
        suppose("There is a task in process") {
            createTask(
                status = BlockchainTaskStatus.IN_PROCESS,
                hash = hash,
                updatedAt = zonedDateTimeProvider.getZonedDateTime()
            )
        }

        suppose("Transaction is not mined") {
            given(blockchainService.isMined(anyValueClass(TransactionHash("")), anyValueClass(ChainId(0L))))
                .willReturn(false)
        }

        verify("Task will remain in process") {
            processAllTasks(maxAttempts = 1)

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(1)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.IN_PROCESS)
            assertThat(tasks[0].hash).isEqualTo(hash.value)
            assertThat(tasks[0].payload).isEqualTo(payload)
        }
    }

    @Test
    fun mustHandleTaskInProcessAndNewTask() {
        suppose("Blockchain service will successfully handle transaction for any address") {
            mockBlockchainTaskSuccessfulResponse()
        }

        suppose("Transactions are mined") {
            given(blockchainService.isMined(anyValueClass(TransactionHash("")), anyValueClass(ChainId(0L))))
                .willReturn(true)
        }

        var task: BlockchainTask? = null
        suppose("There is a task in process") {
            task = createTask(status = BlockchainTaskStatus.IN_PROCESS, hash = hash)
        }

        suppose("New task is created") {
            createTask()
        }

        verify("Both tasks are handled in correct order") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch {
                it.status == BlockchainTaskStatus.COMPLETED &&
                    it.hash == hash.value &&
                    it.payload == payload
            }

            tasks.sortBy { it.createdAt }

            assertThat(tasks[0].uuid).isEqualTo(task!!.uuid)
            assertThat(tasks[0].updatedAt).isBefore(tasks[1].updatedAt)
        }
    }

    @Test
    fun mustHandleNotMinedTransaction() {
        suppose("Transaction is not mined") {
            given(blockchainService.isMined(anyValueClass(TransactionHash("")), anyValueClass(ChainId(0L))))
                .willReturn(false)
        }

        suppose("There is a task in process") {
            createTask(
                status = BlockchainTaskStatus.IN_PROCESS,
                hash = hash,
                updatedAt = zonedDateTimeProvider.getZonedDateTime()
                    .minusMinutes(applicationProperties.queue.miningPeriod * 2)
            )
        }

        verify("Task will fail and new task will be created") {
            processAllTasks(maxAttempts = 1)

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(tasks[0].hash).isEqualTo(hash.value)
            assertThat(tasks[0].payload).isEqualTo(payload)
            assertThat(tasks[1].status).isEqualTo(BlockchainTaskStatus.CREATED)
            assertThat(tasks[1].payload).isEqualTo(payload)
        }
    }

    @Test
    fun mustStartTaskAfterFailedTask() {
        val failedHash = TransactionHash("failed_hash")
        suppose("Transaction is not mined") {
            given(blockchainService.isMined(failedHash.mockito(), chainId.mockito())).willReturn(false)
        }

        suppose("There is a task in process") {
            createTask(
                addresses = listOf(addresses.first()),
                status = BlockchainTaskStatus.IN_PROCESS,
                hash = failedHash,
                updatedAt = zonedDateTimeProvider.getZonedDateTime()
                    .minusMinutes(applicationProperties.queue.miningPeriod * 2)
            )
        }

        suppose("Blockchain service will mine transaction") {
            mockBlockchainTaskSuccessfulResponse()
        }

        suppose("New transaction will be mined") {
            given(blockchainService.isMined(hash.mockito(), chainId.mockito())).willReturn(true)
        }

        var task: BlockchainTask? = null
        suppose("New task is created") {
            task = createTask()
        }

        verify("First task failed") {
            processAllTasks()

            val failedTask = blockchainTaskRepository.findAll().firstOrNull { it.uuid != task!!.uuid }
                ?: fail("Missing failed transaction")

            assertThat(failedTask.status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(failedTask.hash).isEqualTo(failedHash.value)
            assertThat(failedTask.payload).isEqualTo(payload)
        }

        verify("Second task is completed") {
            processAllTasks()

            val successfulTask = blockchainTaskRepository.findById(task!!.uuid).unwrap()
                ?: fail("Missing transaction")

            assertThat(successfulTask.status).isEqualTo(BlockchainTaskStatus.COMPLETED)
            assertThat(successfulTask.hash).isEqualTo(hash.value)
            assertThat(successfulTask.payload).isEqualTo(payload)
        }
    }

    @Test
    fun mustHandleMultipleTasksInOrder() {
        suppose("Blockchain service will successfully handle transaction for any address") {
            mockBlockchainTaskSuccessfulResponse()
        }

        suppose("Transactions are mined") {
            given(blockchainService.isMined(anyValueClass(TransactionHash("")), anyValueClass(ChainId(0L))))
                .willReturn(true)
        }

        suppose("There are multiple tasks") {
            for (i in 1..10) {
                createTask()
            }
        }

        verify("Tasks are handled in order") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()
            tasks.sortBy { it.createdAt }

            assertThat(tasks).hasSize(10)
            assertThat(tasks).allMatch {
                it.status == BlockchainTaskStatus.COMPLETED &&
                    it.hash == hash.value &&
                    it.payload == payload
            }

            for (i in 0..8) {
                assertThat(tasks[i].createdAt).isBefore(tasks[i + 1].createdAt)
                assertThat(tasks[i].updatedAt).isBefore(tasks[i + 1].updatedAt)
            }
        }
    }

    private tailrec fun processAllTasks(maxAttempts: Int = 100) {
        queueScheduler.execute()

        val hasTasksInQueue =
            blockchainTaskRepository.getInProcess() != null || blockchainTaskRepository.getPending() != null
        if (hasTasksInQueue && maxAttempts > 1) {
            processAllTasks(maxAttempts - 1)
        }
    }
}
