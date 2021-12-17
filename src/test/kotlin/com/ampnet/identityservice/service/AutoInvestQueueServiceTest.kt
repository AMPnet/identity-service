package com.ampnet.identityservice.service

import com.ampnet.identityservice.ManualFixedScheduler
import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.blockchain.IInvestService
import com.ampnet.identityservice.blockchain.properties.Chain
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.config.DatabaseCleanerService
import com.ampnet.identityservice.config.TestSchedulerConfiguration
import com.ampnet.identityservice.controller.pojo.request.AutoInvestRequest
import com.ampnet.identityservice.controller.pojo.response.AutoInvestResponse
import com.ampnet.identityservice.persistence.model.AutoInvestTask
import com.ampnet.identityservice.persistence.model.AutoInvestTaskHistoryStatus
import com.ampnet.identityservice.persistence.model.AutoInvestTaskStatus
import com.ampnet.identityservice.persistence.model.AutoInvestTransaction
import com.ampnet.identityservice.persistence.repository.AutoInvestTaskRepository
import com.ampnet.identityservice.persistence.repository.AutoInvestTransactionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import java.math.BigInteger
import java.time.ZonedDateTime

@SpringBootTest
@Import(TestSchedulerConfiguration::class)

class AutoInvestQueueServiceTest : TestBase() {

    private var address1 = "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF7"
    private val address2 = "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF8"
    private val address3 = "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aF9"
    private val campaign1 = "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aFa"
    private val campaign2 = "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aFb"
    private val campaign3 = "0xbdD53fE8b8c2359Ed321b6ef00908fb3e94D0aFc"
    private val hash = "0x6f7dea8d5d98d119de31204dfbdc69bb1944db04891ad0c45ab577da8e6de04a"
    private val chainId = Chain.MATIC_TESTNET_MUMBAI.id

    @Autowired
    private lateinit var databaseCleanerService: DatabaseCleanerService

    @Autowired
    private lateinit var autoInvestTaskRepository: AutoInvestTaskRepository

    @Autowired
    private lateinit var autoInvestTransactionRepository: AutoInvestTransactionRepository

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    @Autowired
    private lateinit var zonedDateTimeProvider: ZonedDateTimeProvider

    @Autowired
    private lateinit var uuidProvider: UuidProvider

    @MockBean
    private lateinit var blockchainService: BlockchainService

    @Autowired
    private lateinit var autoInvestQueueService: AutoInvestQueueService

    @Autowired
    private lateinit var autoInvestQueueScheduler: ManualFixedScheduler

    @BeforeEach
    fun beforeEach() {
        databaseCleanerService.deleteAllAutoInvestTasks()
        databaseCleanerService.deleteAllAutoInvestTransactions()
    }

    @AfterEach
    fun afterEach() {
        databaseCleanerService.deleteAllAutoInvestTasks()
        databaseCleanerService.deleteAllAutoInvestTransactions()
    }

    @Test
    fun mustCorrectlyCreateAutoInvestTask() {
        var response: AutoInvestResponse? = null
        suppose("Auto-invest task creation is requested") {
            response = autoInvestQueueService.createOrUpdateAutoInvestTask(
                address = address1,
                campaign = campaign1,
                chainId = chainId,
                request = AutoInvestRequest(amount = BigInteger.valueOf(100L))
            )
        }

        verify("Auto-invest task is created") {
            assertThat(response).isNotNull()
            assertThat(response).isEqualTo(
                AutoInvestResponse(
                    walletAddress = address1,
                    campaignAddress = campaign1,
                    amount = BigInteger.valueOf(100L)
                )
            )

            val databaseTask = autoInvestTaskRepository.findByUserWalletAddressAndCampaignContractAddressAndChainId(
                userWalletAddress = address1,
                campaignContractAddress = campaign1,
                chainId = chainId
            )

            assertThat(databaseTask).isNotNull()
            assertThat(databaseTask?.userWalletAddress).isEqualTo(address1)
            assertThat(databaseTask?.campaignContractAddress).isEqualTo(campaign1)
            assertThat(databaseTask?.chainId).isEqualTo(chainId)
            assertThat(databaseTask?.amount).isEqualTo(BigInteger.valueOf(100L))
            assertThat(databaseTask?.status).isEqualTo(AutoInvestTaskStatus.PENDING)
        }
    }

    @Test
    fun mustCorrectlyUpdateAutoInvestTask() {
        lateinit var task: AutoInvestTask
        suppose("Pending auto-invest task exists in the database") {
            task = createAutoInvestTask(
                address = address1,
                campaign = campaign1,
                chain = chainId,
                amount = 400L
            )
        }

        var response: AutoInvestResponse? = null
        suppose("Auto-invest task update is requested") {
            response = autoInvestQueueService.createOrUpdateAutoInvestTask(
                address = address1,
                campaign = campaign1,
                chainId = chainId,
                request = AutoInvestRequest(amount = BigInteger.valueOf(600L))
            )
        }

        verify("Auto-invest task is updated") {
            assertThat(response).isNotNull()
            assertThat(response).isEqualTo(
                AutoInvestResponse(
                    walletAddress = address1,
                    campaignAddress = campaign1,
                    amount = BigInteger.valueOf(1_000L)
                )
            )

            val databaseTask = autoInvestTaskRepository.findById(task.uuid)
            assertThat(databaseTask).hasValueSatisfying {
                assertThat(it.userWalletAddress).isEqualTo(address1)
                assertThat(it.campaignContractAddress).isEqualTo(campaign1)
                assertThat(it.chainId).isEqualTo(chainId)
                assertThat(it.amount).isEqualTo(BigInteger.valueOf(1_000L))
                assertThat(it.status).isEqualTo(AutoInvestTaskStatus.PENDING)
            }
        }
    }

    @Test
    fun mustNotUpdateInProcessTaskAndReturnNull() {
        lateinit var task: AutoInvestTask
        suppose("In process auto-invest task exists in the database") {
            task = createAutoInvestTask(
                address = address1,
                campaign = campaign1,
                chain = chainId,
                amount = 400L,
                status = AutoInvestTaskStatus.IN_PROCESS
            )
        }

        var response: AutoInvestResponse? = null
        suppose("Auto-invest task update is requested") {
            response = autoInvestQueueService.createOrUpdateAutoInvestTask(
                address = address1,
                campaign = campaign1,
                chainId = chainId,
                request = AutoInvestRequest(amount = BigInteger.valueOf(600L))
            )
        }

        verify("Auto-invest task is not updated") {
            assertThat(response).isNull()

            val databaseTask = autoInvestTaskRepository.findById(task.uuid)
            assertThat(databaseTask).hasValueSatisfying {
                assertThat(it.userWalletAddress).isEqualTo(address1)
                assertThat(it.campaignContractAddress).isEqualTo(campaign1)
                assertThat(it.chainId).isEqualTo(chainId)
                assertThat(it.amount).isEqualTo(BigInteger.valueOf(400L))
                assertThat(it.status).isEqualTo(AutoInvestTaskStatus.IN_PROCESS)
            }
        }
    }

    @Test
    fun mustDeleteSingleTooOldPendingAutoInvestTask() {
        lateinit var task: AutoInvestTask
        suppose("There is pending auto-invest task which is too old") {
            task = createAutoInvestTask(
                createdAt = zonedDateTimeProvider.getZonedDateTime()
                    .minus(applicationProperties.autoInvest.timeout.multipliedBy(2L))
            )
        }

        verify("Too old pending task is deleted") {
            processAllTasks()

            assertThat(autoInvestTaskRepository.findById(task.uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.getHistoricalUuidsForStatus(AutoInvestTaskHistoryStatus.EXPIRED))
                .containsExactly(task.uuid)
        }
    }

    @Test
    fun mustDeleteMultipleTooOldPendingAutoInvestTasks() {
        lateinit var task1: AutoInvestTask
        lateinit var task2: AutoInvestTask
        lateinit var task3: AutoInvestTask
        suppose("There are pending auto-invest tasks which are too old") {
            task1 = createAutoInvestTask(
                address = address1,
                createdAt = zonedDateTimeProvider.getZonedDateTime()
                    .minus(applicationProperties.autoInvest.timeout.multipliedBy(2L))
            )
            task2 = createAutoInvestTask(
                address = address2,
                createdAt = zonedDateTimeProvider.getZonedDateTime()
                    .minus(applicationProperties.autoInvest.timeout.multipliedBy(2L))
            )
            task3 = createAutoInvestTask(
                address = address3,
                createdAt = zonedDateTimeProvider.getZonedDateTime()
                    .minus(applicationProperties.autoInvest.timeout.multipliedBy(2L))
            )
        }

        verify("Too old pending tasks are deleted") {
            processAllTasks()

            assertThat(autoInvestTaskRepository.findById(task1.uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.findById(task2.uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.findById(task3.uuid)).isEmpty()

            assertThat(autoInvestTaskRepository.getHistoricalUuidsForStatus(AutoInvestTaskHistoryStatus.EXPIRED))
                .containsExactlyInAnyOrder(task1.uuid, task2.uuid, task3.uuid)
        }
    }

    @Test
    fun mustNotChangeTaskStatusWhenReturnedHashIsNull() {
        lateinit var task: AutoInvestTask
        suppose("There is pending auto-invest task") {
            task = createAutoInvestTask()
        }

        suppose("Blockchain service will report task as ready to invest") {
            given(blockchainService.getAutoInvestStatus(any(), any())).willReturn(
                listOf(task.isReadyForAutoInvest(true))
            )
        }

        suppose("Blockchain service will return null for hash") {
            given(blockchainService.autoInvestFor(any(), any())).willReturn(null)
        }

        verify("Task status is not changed") {
            processAllTasks()

            val databaseTask = autoInvestTaskRepository.findById(task.uuid)
            assertThat(databaseTask).hasValueSatisfying {
                assertThat(it.status).isEqualTo(AutoInvestTaskStatus.PENDING)
            }
        }
    }

    @Test
    fun mustDeleteMinedInProcessTask() {
        lateinit var task: AutoInvestTask
        suppose("There is auto-invest task with hash in process") {
            task = createAutoInvestTask(status = AutoInvestTaskStatus.IN_PROCESS, hash = hash)
        }

        suppose("Transaction is mined for task hash") {
            given(blockchainService.isMined(hash, chainId)).willReturn(true)
        }

        verify("Mined in process task is deleted") {
            processAllTasks()

            assertThat(autoInvestTaskRepository.findById(task.uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.getHistoricalUuidsForStatus(AutoInvestTaskHistoryStatus.SUCCESS))
                .containsExactly(task.uuid)
        }
    }

    @Test
    fun mustCorrectlyHandleInProcessTaskWithDelayedBlockMiningPeriod() {
        lateinit var task: AutoInvestTask
        suppose("There is auto-invest task with hash in process") {
            task = createAutoInvestTask(status = AutoInvestTaskStatus.IN_PROCESS, hash = hash)
        }

        suppose("There is auto-invest transaction in the database") {
            autoInvestTransactionRepository.save(
                AutoInvestTransaction(chainId, hash, uuidProvider, zonedDateTimeProvider)
            )
        }

        suppose("Transaction is not yet mined for task hash") {
            given(blockchainService.isMined(hash, chainId)).willReturn(false)
        }

        verify("Task is still in process") {
            processAllTasks(maxAttempts = 1)
            assertThat(autoInvestTaskRepository.findById(task.uuid)).hasValueSatisfying {
                assertThat(it.status).isEqualTo(AutoInvestTaskStatus.IN_PROCESS)
            }
        }

        suppose("Transaction is mined for task hash") {
            given(blockchainService.isMined(hash, chainId)).willReturn(true)
        }

        verify("Mined in process task is deleted") {
            processAllTasks(maxAttempts = 1)

            assertThat(autoInvestTaskRepository.findById(task.uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.getHistoricalUuidsForStatus(AutoInvestTaskHistoryStatus.SUCCESS))
                .containsExactly(task.uuid)
        }
    }

    @Test
    fun mustCorrectlyHandleInProcessTaskWithDelayedBlockMiningPeriodWhenTransactionIsTooOld() {
        lateinit var task: AutoInvestTask
        suppose("There is auto-invest task with hash in process") {
            task = createAutoInvestTask(status = AutoInvestTaskStatus.IN_PROCESS, hash = hash)
        }

        suppose("There is too old auto-invest transaction in the database") {
            autoInvestTransactionRepository.save(
                AutoInvestTransaction(
                    uuidProvider.getUuid(),
                    chainId,
                    hash,
                    zonedDateTimeProvider.getZonedDateTime()
                        .minusSeconds(applicationProperties.autoInvest.queue.miningPeriod * 2)
                )
            )
        }

        suppose("Transaction is not yet mined for task hash") {
            given(blockchainService.isMined(hash, chainId)).willReturn(false)
        }

        verify("Task is deleted") {
            processAllTasks(maxAttempts = 1)

            assertThat(autoInvestTaskRepository.findById(task.uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.getHistoricalUuidsForStatus(AutoInvestTaskHistoryStatus.FAILURE))
                .containsExactly(task.uuid)
        }
    }

    @Test
    fun mustCorrectlyProcessPendingTasksBasedOnInvestmentReadiness() {
        lateinit var tasks: List<AutoInvestTask>
        suppose("There are some pending auto-invest tasks") {
            tasks = listOf(
                createAutoInvestTask(address = address1, campaign = campaign1),
                createAutoInvestTask(address = address2, campaign = campaign1),
                createAutoInvestTask(address = address3, campaign = campaign1),
                createAutoInvestTask(address = address1, campaign = campaign2),
                createAutoInvestTask(address = address2, campaign = campaign2),
                createAutoInvestTask(address = address3, campaign = campaign2)
            )
        }

        suppose("Some tasks will be marked as ready and others as not ready") {
            given(blockchainService.getAutoInvestStatus(any(), any()))
                .willReturn(
                    listOf(
                        tasks[0].isReadyForAutoInvest(true),
                        tasks[1].isReadyForAutoInvest(true),
                        tasks[2].isReadyForAutoInvest(true),
                        tasks[3].isReadyForAutoInvest(false),
                        tasks[4].isReadyForAutoInvest(false),
                        tasks[5].isReadyForAutoInvest(false)
                    )
                )
        }

        suppose("Auto-invest is requested for ready tasks") {
            val readyTasks = tasks.take(3).map { it.toRecord() }
            given(blockchainService.autoInvestFor(readyTasks, chainId)).willReturn(hash)
        }

        suppose("Transaction is successfully mined") {
            given(blockchainService.isMined(hash, chainId)).willReturn(true)
        }

        verify("Only ready to invest tasks are processed") {
            processAllTasks()

            assertThat(autoInvestTaskRepository.findById(tasks[0].uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.findById(tasks[1].uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.findById(tasks[2].uuid)).isEmpty()

            assertThat(autoInvestTaskRepository.findById(tasks[3].uuid)).hasValueSatisfying {
                assertThat(it.status).isEqualTo(AutoInvestTaskStatus.PENDING)
            }
            assertThat(autoInvestTaskRepository.findById(tasks[4].uuid)).hasValueSatisfying {
                assertThat(it.status).isEqualTo(AutoInvestTaskStatus.PENDING)
            }
            assertThat(autoInvestTaskRepository.findById(tasks[5].uuid)).hasValueSatisfying {
                assertThat(it.status).isEqualTo(AutoInvestTaskStatus.PENDING)
            }

            assertThat(autoInvestTaskRepository.getHistoricalUuidsForStatus(AutoInvestTaskHistoryStatus.SUCCESS))
                .containsExactlyInAnyOrderElementsOf(tasks.take(3).map { it.uuid })
        }
    }

    @Test
    fun mustCorrectlyProcessSingleTaskForSingleChain() {
        lateinit var task: AutoInvestTask
        suppose("There is pending auto-invest task") {
            task = createAutoInvestTask()
        }

        suppose("Task is marked as ready") {
            given(blockchainService.getAutoInvestStatus(any(), any()))
                .willReturn(listOf(task.isReadyForAutoInvest(true)))
        }

        suppose("Blockchain service will return some hash") {
            given(blockchainService.autoInvestFor(listOf(task.toRecord()), chainId)).willReturn(hash)
        }

        suppose("Transaction is successfully mined") {
            given(blockchainService.isMined(hash, chainId)).willReturn(true)
        }

        verify("Task is processed") {
            processAllTasks()

            assertThat(autoInvestTaskRepository.findById(task.uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.getHistoricalUuidsForStatus(AutoInvestTaskHistoryStatus.SUCCESS))
                .containsExactly(task.uuid)
        }
    }

    @Test
    fun mustCorrectlyProcessMultipleTasksForSingleChain() {
        lateinit var task1: AutoInvestTask
        lateinit var task2: AutoInvestTask
        lateinit var task3: AutoInvestTask
        suppose("There are pending auto-invest tasks") {
            task1 = createAutoInvestTask(address = address1, campaign = campaign1)
            task2 = createAutoInvestTask(address = address1, campaign = campaign2)
            task3 = createAutoInvestTask(address = address1, campaign = campaign3)
        }

        suppose("Tasks are marked as ready") {
            given(blockchainService.getAutoInvestStatus(any(), any()))
                .willReturn(
                    listOf(
                        task1.isReadyForAutoInvest(true),
                        task2.isReadyForAutoInvest(true),
                        task3.isReadyForAutoInvest(true)
                    )
                )
        }

        suppose("Blockchain service will return some hash") {
            given(
                blockchainService.autoInvestFor(
                    listOf(task1.toRecord(), task2.toRecord(), task3.toRecord()),
                    chainId
                )
            ).willReturn(hash)
        }

        suppose("Transaction is successfully mined") {
            given(blockchainService.isMined(hash, chainId)).willReturn(true)
        }

        verify("Tasks are processed") {
            processAllTasks()

            assertThat(autoInvestTaskRepository.findById(task1.uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.findById(task2.uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.findById(task3.uuid)).isEmpty()

            assertThat(autoInvestTaskRepository.getHistoricalUuidsForStatus(AutoInvestTaskHistoryStatus.SUCCESS))
                .containsExactlyInAnyOrder(task1.uuid, task2.uuid, task3.uuid)
        }
    }

    @Test
    fun mustCorrectlyProcessMultipleTasksForMultipleChains() {
        lateinit var tasks: List<AutoInvestTask>
        suppose("There are some pending auto-invest tasks") {
            tasks = listOf(
                createAutoInvestTask(address = address1, campaign = campaign1, chain = 1L),
                createAutoInvestTask(address = address2, campaign = campaign2, chain = 1L),
                createAutoInvestTask(address = address3, campaign = campaign3, chain = 2L),
                createAutoInvestTask(address = address1, campaign = campaign1, chain = 2L),
                createAutoInvestTask(address = address2, campaign = campaign2, chain = 3L),
                createAutoInvestTask(address = address3, campaign = campaign3, chain = 3L)
            )
        }

        suppose("Tasks are marked as ready") {
            given(blockchainService.getAutoInvestStatus(any(), any()))
                .willReturn(
                    listOf(
                        tasks[0].isReadyForAutoInvest(true),
                        tasks[1].isReadyForAutoInvest(true),
                        tasks[2].isReadyForAutoInvest(true),
                        tasks[3].isReadyForAutoInvest(true),
                        tasks[4].isReadyForAutoInvest(true),
                        tasks[5].isReadyForAutoInvest(true)
                    )
                )
        }

        val chain1Tasks = listOf(tasks[0], tasks[1]).map { it.toRecord() }
        val chain2Tasks = listOf(tasks[2], tasks[3]).map { it.toRecord() }
        val chain3Tasks = listOf(tasks[4], tasks[5]).map { it.toRecord() }
        val hash1 = "hash1"
        val hash2 = "hash2"
        val hash3 = "hash3"
        suppose("Blockchain service will return different hash for each chain") {
            given(blockchainService.autoInvestFor(chain1Tasks, 1L)).willReturn(hash1)
            given(blockchainService.autoInvestFor(chain2Tasks, 2L)).willReturn(hash2)
            given(blockchainService.autoInvestFor(chain3Tasks, 3L)).willReturn(hash3)
        }

        suppose("Transactions are successfully mined") {
            given(blockchainService.isMined(hash1, 1L)).willReturn(true)
            given(blockchainService.isMined(hash2, 2L)).willReturn(true)
            given(blockchainService.isMined(hash3, 3L)).willReturn(true)
        }

        verify("Tasks are processed") {
            processAllTasks()

            assertThat(autoInvestTaskRepository.findById(tasks[0].uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.findById(tasks[1].uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.findById(tasks[2].uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.findById(tasks[3].uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.findById(tasks[4].uuid)).isEmpty()
            assertThat(autoInvestTaskRepository.findById(tasks[5].uuid)).isEmpty()

            assertThat(autoInvestTaskRepository.getHistoricalUuidsForStatus(AutoInvestTaskHistoryStatus.SUCCESS))
                .containsExactlyInAnyOrderElementsOf(tasks.map { it.uuid })
        }
    }

    private tailrec fun processAllTasks(maxAttempts: Int = 100) {
        autoInvestQueueScheduler.execute()

        val hasTasksInQueue = autoInvestTaskRepository.findByStatus(AutoInvestTaskStatus.IN_PROCESS).isNotEmpty() ||
            autoInvestTaskRepository.findByStatus(AutoInvestTaskStatus.PENDING).isNotEmpty()
        if (hasTasksInQueue && maxAttempts > 1) {
            processAllTasks(maxAttempts - 1)
        }
    }

    private fun createAutoInvestTask(
        status: AutoInvestTaskStatus = AutoInvestTaskStatus.PENDING,
        address: String = address1,
        campaign: String = campaign1,
        chain: Long = chainId,
        amount: Long = 100L,
        hash: String? = null,
        createdAt: ZonedDateTime = zonedDateTimeProvider.getZonedDateTime()
    ): AutoInvestTask {
        val task = AutoInvestTask(
            uuidProvider.getUuid(),
            chain,
            address,
            campaign,
            BigInteger.valueOf(amount),
            status,
            hash,
            createdAt
        )
        return autoInvestTaskRepository.save(task)
    }

    private fun AutoInvestTask.isReadyForAutoInvest(readiness: Boolean) =
        IInvestService.InvestmentRecordStatus(
            this.userWalletAddress,
            this.campaignContractAddress,
            this.amount,
            readiness
        )

    private fun AutoInvestTask.toRecord() =
        IInvestService.InvestmentRecord(this.userWalletAddress, this.campaignContractAddress, this.amount)
}
