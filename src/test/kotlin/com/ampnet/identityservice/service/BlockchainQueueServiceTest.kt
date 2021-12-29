package com.ampnet.identityservice.service

import com.ampnet.identityservice.ManualFixedScheduler
import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.blockchain.properties.Chain
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.config.DatabaseCleanerService
import com.ampnet.identityservice.controller.pojo.request.WhitelistRequest
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

    private val addresses = listOf(
        "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF7",
        "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF8",
        "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF9"
    )
    private val hash = "0x6f7dea8d5d98d119de31204dfbdc69bb1944db04891ad0c45ab577da8e6de04a"
    private val issuerAddress = "0xb070a65b1dd7f49c90a59000bd8cca3259064d81"
    private val chainId = Chain.MATIC_TESTNET_MUMBAI.id
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

    @Autowired
    private lateinit var whitelistQueueScheduler: ManualFixedScheduler

    @BeforeEach
    fun beforeEach() {
        testContext = TestContext()
        databaseCleanerService.deleteAllBlockchainTasks()
        databaseCleanerService.deleteAllQueuedBlockchainAddresses()
        given(blockchainService.isWhitelisted(any(), any(), any())).willReturn(true)
    }

    @AfterEach
    fun afterEach() {
        databaseCleanerService.deleteAllBlockchainTasks()
        databaseCleanerService.deleteAllQueuedBlockchainAddresses()
    }

    @Test
    fun mustHandleSingleTaskInQueue() {
        suppose("Blockchain service will whitelist address") {
            given(blockchainService.whitelistAddresses(addresses, issuerAddress, chainId)).willReturn(hash)
        }

        suppose("Transaction is mined") {
            given(blockchainService.isMined(hash, chainId)).willReturn(true)
        }

        suppose("There is a task in queue") {
            testContext.task = createBlockchainTask()
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
        suppose("Blockchain service will whitelist any address") {
            given(blockchainService.whitelistAddresses(any(), any(), any())).willReturn(hash)
        }

        suppose("Transactions are mined") {
            given(blockchainService.isMined(any(), any())).willReturn(true)
        }

        suppose("There are two tasks in queue") {
            testContext.task = createBlockchainTask()
            createBlockchainTask()
        }

        verify("Service will handle the task") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED && it.hash == hash }
        }
    }

    @Test
    fun mustFailPendingTaskWhenExceptionIsThrown() {
        suppose("Blockchain service will throw exception") {
            given(blockchainService.whitelistAddresses(any(), any(), any())).willThrow(RuntimeException())
        }

        suppose("There is a task in queue") {
            createBlockchainTask(status = BlockchainTaskStatus.CREATED)
        }

        verify("Task will fail and new task is created") {
            processAllTasks(maxAttempts = 1)

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(tasks[1].status).isEqualTo(BlockchainTaskStatus.CREATED)
            assertThat(tasks[0].addresses).isEqualTo(tasks[1].addresses)
            assertThat(tasks[0].payload).isEqualTo(tasks[1].payload)
        }
    }

    @Test
    fun mustFailInProcessTaskWhenExceptionIsThrown() {
        suppose("Blockchain service will throw exception") {
            given(blockchainService.isMined(hash, chainId)).willThrow(RuntimeException())
        }

        suppose("There is a task in process") {
            createBlockchainTask(status = BlockchainTaskStatus.IN_PROCESS, hash = hash)
        }

        verify("Task will fail and new task is created") {
            processAllTasks(maxAttempts = 1)

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(tasks[0].hash).isEqualTo(hash)
            assertThat(tasks[1].status).isEqualTo(BlockchainTaskStatus.CREATED)
            assertThat(tasks[0].addresses).isEqualTo(tasks[1].addresses)
            assertThat(tasks[0].payload).isEqualTo(tasks[1].payload)
        }
    }

    @Test
    fun mustHandleSingleTaskInQueueWhenFirstReturnedHashIsNull() {
        suppose("Blockchain service will not send whitelist address") {
            given(blockchainService.whitelistAddresses(any(), any(), any())).willReturn(null)
        }

        suppose("There is a task in queue") {
            createBlockchainTask()
        }

        verify("Task will remain in queue") {
            processAllTasks(maxAttempts = 10)

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(1)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.CREATED)
            assertThat(tasks[0].hash).isNull()
        }

        suppose("Blockchain service will whitelist any address") {
            given(blockchainService.whitelistAddresses(any(), any(), any())).willReturn(hash)
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
    fun mustHandleTaskInProcessAndNewTask() {
        suppose("Blockchain service will whitelist any address") {
            given(blockchainService.whitelistAddresses(any(), any(), any())).willReturn(hash)
        }

        suppose("Transactions are mined") {
            given(blockchainService.isMined(any(), any())).willReturn(true)
        }

        suppose("There is task in process") {
            testContext.task = createBlockchainTask(status = BlockchainTaskStatus.IN_PROCESS, hash = hash)
        }

        suppose("New task is created") {
            createBlockchainTask()
        }

        verify("Both tasks are handled in correct order") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED && it.hash == hash }

            tasks.sortBy { it.createdAt }

            assertThat(tasks[0].uuid).isEqualTo(testContext.task.uuid)
            assertThat(tasks[0].updatedAt).isBefore(tasks[1].updatedAt)
        }
    }

    @Test
    fun mustHandleNotMinedTransaction() {
        suppose("Transaction is not mined") {
            given(blockchainService.isMined(any(), any())).willReturn(false)
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
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()
            assertThat(tasks).hasSize(2)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(tasks[0].hash).isEqualTo(hash)
            assertThat(tasks[1].status).isEqualTo(BlockchainTaskStatus.CREATED)
            assertThat(tasks[0].addresses).isEqualTo(tasks[1].addresses)
            assertThat(tasks[0].payload).isEqualTo(tasks[1].payload)
        }
    }

    @Test
    fun mustHandleInProcessTaskWhichExceededMiningPeriod() {
        suppose("There is a task in process which exceeds mining period") {
            createBlockchainTask(
                status = BlockchainTaskStatus.IN_PROCESS,
                hash = hash,
                updatedAt = zonedDateTimeProvider.getZonedDateTime()
                    .minusSeconds(applicationProperties.queue.miningPeriod * 2)
            )
        }

        verify("Task will fail and new task is created") {
            processAllTasks(maxAttempts = 1)

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(tasks[0].hash).isEqualTo(hash)
            assertThat(tasks[1].status).isEqualTo(BlockchainTaskStatus.CREATED)
            assertThat(tasks[0].addresses).isEqualTo(tasks[1].addresses)
            assertThat(tasks[0].payload).isEqualTo(tasks[1].payload)
        }
    }

    @Test
    fun mustHandleInProcessWhichIsWaitingForTransactionToBeMined() {
        suppose("There is a task in process") {
            createBlockchainTask(
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
            assertThat(tasks[0].addresses).isEqualTo(addresses)
            assertThat(tasks[0].payload).isEqualTo(issuerAddress)
        }
    }

    @Test
    fun mustStartTaskAfterFailedTask() {
        suppose("Transaction is not mined") {
            given(blockchainService.isMined("some_hash", chainId)).willReturn(false)
        }

        suppose("There is task in process") {
            createBlockchainTask(
                addresses = listOf("some_address"),
                status = BlockchainTaskStatus.IN_PROCESS,
                hash = "some_hash",
                updatedAt = zonedDateTimeProvider.getZonedDateTime()
                    .minusMinutes(applicationProperties.queue.miningPeriod * 2)
            )
        }

        suppose("Blockchain service will mine transaction") {
            given(blockchainService.whitelistAddresses(addresses, issuerAddress, chainId)).willReturn(hash)
        }

        suppose("New transaction will be mined") {
            given(blockchainService.isMined(hash, chainId)).willReturn(true)
        }

        suppose("New task is created") {
            testContext.task = createBlockchainTask()
        }

        verify("First task failed") {
            processAllTasks()

            val task = blockchainTaskRepository.findAll().firstOrNull { it.uuid != testContext.task.uuid }
                ?: fail("Missing failed transaction")

            assertThat(task.status).isEqualTo(BlockchainTaskStatus.FAILED)
            assertThat(task.hash).isEqualTo("some_hash")
            assertThat(task.addresses).isEqualTo(addresses)
            assertThat(task.payload).isEqualTo(issuerAddress)
        }

        verify("Second task is completed") {
            processAllTasks()

            val task = blockchainTaskRepository.findById(testContext.task.uuid).unwrap()
                ?: fail("Missing transaction")

            assertThat(task.status).isEqualTo(BlockchainTaskStatus.COMPLETED)
            assertThat(task.hash).isEqualTo(hash)
            assertThat(task.addresses).isEqualTo(addresses)
            assertThat(task.payload).isEqualTo(issuerAddress)
        }
    }

    @Test
    fun mustHandleMultipleTasksInOrder() {
        suppose("Blockchain service will whitelist any address") {
            given(blockchainService.whitelistAddresses(any(), any(), any())).willReturn(hash)
        }

        suppose("Transactions are mined") {
            given(blockchainService.isMined(any(), any())).willReturn(true)
        }

        suppose("There are multiple tasks") {
            for (i in 1..10) {
                createBlockchainTask()
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

    @Test
    fun mustNotCreateNewTaskForWhitelistedAddress() {
        suppose("There is whitelisted address for issuer") {
            createBlockchainTask(BlockchainTaskStatus.COMPLETED, hash = hash)
        }

        suppose("Whitelisting request is received") {
            queueService.createWhitelistAddressTask(addresses.first(), WhitelistRequest(issuerAddress, chainId))
        }

        verify("Service will not create a new task") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(1)
            assertThat(tasks[0].status).isEqualTo(BlockchainTaskStatus.COMPLETED)
            assertThat(tasks[0].hash).isEqualTo(hash)
            assertThat(tasks[0].addresses).isEqualTo(addresses)
            assertThat(tasks[0].payload).isEqualTo(issuerAddress)
        }
    }

    @Test
    fun mustCreateNewTaskForFailedWhitelistedAddress() {
        suppose("There is whitelisted address for issuer") {
            createBlockchainTask(BlockchainTaskStatus.FAILED)
        }

        suppose("Blockchain service will mine transaction") {
            given(blockchainService.whitelistAddresses(addresses, issuerAddress, chainId)).willReturn(hash)
        }

        suppose("New transaction will be mined") {
            given(blockchainService.isMined(hash, chainId)).willReturn(true)
        }

        suppose("Whitelisting request is received") {
            queueService.createWhitelistAddressTask(addresses.first(), WhitelistRequest(issuerAddress, chainId))
        }

        verify("Service created new task") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            // TODO: size is 1
            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.payload == addresses.first() }
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
            given(blockchainService.whitelistAddresses(addresses, "new_issuer_address", chainId))
                .willReturn(hash)
        }

        suppose("New transaction will be mined") {
            given(blockchainService.isMined(hash, chainId)).willReturn(true)
        }

        verify("New task is handled") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED && it.payload == addresses.first() }
            assertThat(tasks.map { it.payload }).containsAll(listOf(issuerAddress, "new_issuer_address"))
        }
    }

    @Test
    fun mustCreateNewTaskForWhitelistedWalletOnNewChain() {
        suppose("There is whitelisted address for issuer") {
            createBlockchainTask(BlockchainTaskStatus.COMPLETED, hash = hash)
        }

        suppose("There is request for whitelisting the address on new chain") {
            createBlockchainTask(BlockchainTaskStatus.CREATED, chain = Chain.MATIC_MAIN.id)
        }

        suppose("Blockchain service will mine transaction") {
            given(blockchainService.whitelistAddresses(addresses, issuerAddress, Chain.MATIC_MAIN.id)).willReturn(hash)
        }

        suppose("New transaction will be mined") {
            given(blockchainService.isMined(hash, Chain.MATIC_MAIN.id)).willReturn(true)
        }

        verify("New task is handled") {
            processAllTasks()

            val tasks = blockchainTaskRepository.findAll()

            assertThat(tasks).hasSize(2)
            assertThat(tasks).allMatch { it.status == BlockchainTaskStatus.COMPLETED && it.payload == addresses.first() }
            assertThat(tasks.map { it.chainId }).containsAll(listOf(chainId, Chain.MATIC_MAIN.id))
        }
    }

    private tailrec fun processAllTasks(maxAttempts: Int = 100) {
        whitelistQueueScheduler.execute()

        val hasTasksInQueue = blockchainTaskRepository.getInProcess() != null ||
            blockchainTaskRepository.getPending() != null
        if (hasTasksInQueue && maxAttempts > 1) {
            processAllTasks(maxAttempts - 1)
        }
    }

    private fun createBlockchainTask(
        status: BlockchainTaskStatus = BlockchainTaskStatus.CREATED,
        addresses: List<String> = this.addresses,
        contractAddress: String = issuerAddress,
        chain: Long = chainId,
        hash: String? = null,
        updatedAt: ZonedDateTime? = null
    ): BlockchainTask {
        val task = BlockchainTask(
            uuidProvider.getUuid(),
            addresses.toTypedArray(),
            chain,
            status,
            contractAddress,
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
