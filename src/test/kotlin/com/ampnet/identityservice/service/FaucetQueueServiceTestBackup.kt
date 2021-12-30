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
import org.springframework.context.annotation.Import
import java.time.ZonedDateTime

// TODO: Left as sanity check, remove after confirming that FaucetQueueServiceTest is working as expected
@SpringBootTest
@Import(TestSchedulerConfiguration::class)
class FaucetQueueServiceTestBackup : TestBase() {

    private var address1 = "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF7"
    private val address2 = "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF8"
    private val address3 = "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF9"
    private val addresses = listOf(address1, address2, address3)
    private val hash = "0x6f7dea8d5d98d119de31204dfbdc69bb1944db04891ad0c45ab577da8e6de04a"
    private val chainId = Chain.MATIC_TESTNET_MUMBAI.id

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

    @MockBean
    private lateinit var blockchainService: BlockchainService

    @Autowired
    private lateinit var faucetQueueScheduler: ManualFixedScheduler

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
        val chainId1 = 1L
        val chainId2 = 2L

        suppose("There are some addresses in the queue") {
            addresses.forEach { blockchainTaskRepository.addAddressToQueue(it, chainId1) }
            addresses.forEach { blockchainTaskRepository.addAddressToQueue(it, chainId2) }
        }

        suppose("Blockchain service will send faucet funds to any address") {
            given(blockchainService.sendFaucetFunds(any(), any())).willReturn(hash)
        }

        suppose("Transactions are mined") {
            given(blockchainService.isMined(any(), any())).willReturn(true)
        }

        verify("Service will handle tasks created from address queue") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED && it.hash == hash }
        }
    }

    @Test
    fun mustHandleSingleTaskInQueue() {
        suppose("Blockchain service will send faucet funds") {
            given(blockchainService.sendFaucetFunds(addresses, chainId)).willReturn(hash)
        }

        suppose("Transaction is mined") {
            given(blockchainService.isMined(hash, chainId)).willReturn(true)
        }

        suppose("There is a task in queue") {
            createFaucetTask()
        }

        verify("Service will handle the task") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(1)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.COMPLETED)
            assertThat(tasks[0].hash).isEqualTo(hash)
        }
    }

    @Test
    fun mustRetryPendingTaskWhenExceptionIsThrown() {
        suppose("Blockchain service will throw exception") {
            given(blockchainService.sendFaucetFunds(any(), any())).willThrow(RuntimeException())
        }

        suppose("There is a task in queue") {
            createFaucetTask(status = BlockchainTaskStatus.CREATED)
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
            given(blockchainService.isMined(hash, chainId)).willThrow(RuntimeException())
        }

        suppose("There is a task in process") {
            createFaucetTask(status = BlockchainTaskStatus.IN_PROCESS, hash = hash)
        }

        verify("Task will fail and new task will be created") {
            processAllTasks(maxAttempts = 1)

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(tasks[0].hash).isEqualTo(hash)
            assertThat(tasks[1].status).isEqualTo(BlockchainTaskStatus.CREATED)
        }
    }

    @Test
    fun mustHandleSingleTaskInQueueWhenFirstReturnedHashIsNull() {
        suppose("Blockchain service will not send faucet funds") {
            given(blockchainService.sendFaucetFunds(addresses, chainId)).willReturn(null)
        }

        suppose("There is a task in queue") {
            createFaucetTask()
        }

        verify("Task will remain in queue") {
            processAllTasks(maxAttempts = 10)

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(1)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.CREATED)
            assertThat(tasks[0].hash).isNull()
        }

        suppose("Blockchain service will send faucet funds") {
            given(blockchainService.sendFaucetFunds(addresses, chainId)).willReturn(hash)
        }

        suppose("Transaction is mined") {
            given(blockchainService.isMined(hash, chainId)).willReturn(true)
        }

        verify("Service will handle the task") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(1)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.COMPLETED)
            assertThat(tasks[0].hash).isEqualTo(hash)
        }
    }

    @Test
    fun mustHandleTwoNewTasksInQueue() {
        suppose("Blockchain service will send faucet funds to any address") {
            given(blockchainService.sendFaucetFunds(any(), any())).willReturn(hash)
        }

        suppose("Transactions are mined") {
            given(blockchainService.isMined(any(), any())).willReturn(true)
        }

        suppose("There are two tasks in queue") {
            createFaucetTask()
            createFaucetTask()
        }

        verify("Service will handle the task") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED && it.hash == hash }
        }
    }

    @Test
    fun mustHandleInProcessTaskWhichExceededMiningPeriod() {
        suppose("There is a task in process which exceeds mining period") {
            createFaucetTask(
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
            assertThat(tasks[0].hash).isEqualTo(hash)
            assertThat(tasks[1].status).isEqualTo(BlockchainTaskStatus.CREATED)
        }
    }

    @Test
    fun mustHandleInProcessWhichIsWaitingForTransactionToBeMined() {
        suppose("There is a task in process") {
            createFaucetTask(
                status = BlockchainTaskStatus.IN_PROCESS,
                hash = hash,
                updatedAt = zonedDateTimeProvider.getZonedDateTime()
            )
        }

        suppose("Transaction is not mined") {
            given(blockchainService.isMined(any(), any())).willReturn(false)
        }

        verify("Task will remain in process") {
            processAllTasks(maxAttempts = 1)

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(1)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.IN_PROCESS)
            assertThat(tasks[0].hash).isEqualTo(hash)
        }
    }

    @Test
    fun mustHandleTaskInProcessAndNewTask() {
        suppose("Blockchain service will send faucet funds to any address") {
            given(blockchainService.sendFaucetFunds(any(), any())).willReturn(hash)
        }

        suppose("Transactions are mined") {
            given(blockchainService.isMined(any(), any())).willReturn(true)
        }

        var task: BlockchainTask? = null
        suppose("There is a task in process") {
            task = createFaucetTask(status = BlockchainTaskStatus.IN_PROCESS, hash = hash)
        }

        suppose("New task is created") {
            createFaucetTask()
        }

        verify("Both tasks are handled in correct order") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED && it.hash == hash }

            tasks.sortBy { it.createdAt }

            assertThat(tasks[0].uuid).isEqualTo(task!!.uuid)
            assertThat(tasks[0].updatedAt).isBefore(tasks[1].updatedAt)
        }
    }

    @Test
    fun mustHandleNotMinedTransaction() {
        suppose("Transaction is not mined") {
            given(blockchainService.isMined(any(), any())).willReturn(false)
        }

        suppose("There is a task in process") {
            createFaucetTask(
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
            assertThat(tasks[0].hash).isEqualTo(hash)
            assertThat(tasks[1].status).isEqualTo(BlockchainTaskStatus.CREATED)
        }
    }

    @Test
    fun mustStartTaskAfterFailedTask() {
        val failedHash = "failed_hash"
        suppose("Transaction is not mined") {
            given(blockchainService.isMined(failedHash, chainId)).willReturn(false)
        }

        suppose("There is a task in process") {
            createFaucetTask(
                addresses = listOf(address1),
                status = BlockchainTaskStatus.IN_PROCESS,
                hash = failedHash,
                updatedAt = zonedDateTimeProvider.getZonedDateTime()
                    .minusMinutes(applicationProperties.queue.miningPeriod * 2)
            )
        }

        suppose("Blockchain service will mine transaction") {
            given(blockchainService.sendFaucetFunds(addresses, chainId)).willReturn(hash)
        }

        suppose("New transaction will be mined") {
            given(blockchainService.isMined(hash, chainId)).willReturn(true)
        }

        var task: BlockchainTask? = null
        suppose("New task is created") {
            task = createFaucetTask()
        }

        verify("First task failed") {
            processAllTasks()

            val failedTask = blockchainTaskRepository.findAll().firstOrNull { it.uuid != task!!.uuid }
                ?: fail("Missing failed transaction")

            assertThat(failedTask.status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(failedTask.hash).isEqualTo(failedHash)
        }

        verify("Second task is completed") {
            processAllTasks()

            val successfulTask = blockchainTaskRepository.findById(task!!.uuid).unwrap()
                ?: fail("Missing transaction")

            assertThat(successfulTask.status).isEqualTo(BlockchainTaskStatus.COMPLETED)
            assertThat(successfulTask.hash).isEqualTo(hash)
        }
    }

    @Test
    fun mustHandleMultipleTasksInOrder() {
        suppose("Blockchain service will send faucet funds to any address") {
            given(blockchainService.sendFaucetFunds(any(), any())).willReturn(hash)
        }

        suppose("Transactions are mined") {
            given(blockchainService.isMined(any(), any())).willReturn(true)
        }

        suppose("There are multiple tasks") {
            for (i in 1..10) {
                createFaucetTask()
            }
        }

        verify("Tasks are handled in order") {
            processAllTasks()

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

    private tailrec fun processAllTasks(maxAttempts: Int = 100) {
        faucetQueueScheduler.execute()

        val hasTasksInQueue = blockchainTaskRepository.getInProcess() != null || blockchainTaskRepository.getPending() != null
        if (hasTasksInQueue && maxAttempts > 1) {
            processAllTasks(maxAttempts - 1)
        }
    }

    private fun createFaucetTask(
        status: BlockchainTaskStatus = BlockchainTaskStatus.CREATED,
        addresses: List<String> = listOf(address1, address2, address3),
        chain: Long = chainId,
        hash: String? = null,
        updatedAt: ZonedDateTime? = null
    ): BlockchainTask {
        val task = BlockchainTask(
            uuidProvider.getUuid(),
            addresses.toTypedArray(),
            chain,
            status,
            null,
            hash,
            zonedDateTimeProvider.getZonedDateTime(),
            updatedAt
        )
        return blockchainTaskRepository.save(task)
    }
}
