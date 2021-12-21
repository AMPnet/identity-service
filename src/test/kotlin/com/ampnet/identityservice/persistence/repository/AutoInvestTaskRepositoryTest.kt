package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.persistence.model.AutoInvestTask
import com.ampnet.identityservice.persistence.model.AutoInvestTaskHistoryStatus
import com.ampnet.identityservice.persistence.model.AutoInvestTaskStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigInteger
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@DataJpaTest
@ExtendWith(value = [SpringExtension::class])
@AutoConfigureTestDatabase
class AutoInvestTaskRepositoryTest : TestBase() {

    @Autowired
    private lateinit var autoInvestTaskRepository: AutoInvestTaskRepository

    @Test
    fun mustCorrectlyInsertAutoInvestTask() {
        val task = taskWithAmount(BigInteger.valueOf(123456789L))

        suppose("auto-invest task is inserted into database") {
            val numModified = autoInvestTaskRepository.createOrUpdate(task)
            assertThat(numModified).isOne()
        }

        verify("auto-invest task was correctly inserted into database") {
            val databaseTask = autoInvestTaskRepository.findById(task.uuid)
            assertThat(databaseTask).hasValue(task)
        }
    }

    @Test
    fun mustCorrectlyUpdateExistingAutoInvestTask() {
        val task = taskWithAmount(BigInteger.valueOf(500L))

        suppose("auto-invest task is inserted into database") {
            val numModified = autoInvestTaskRepository.createOrUpdate(task)
            assertThat(numModified).isOne()
        }

        val newTask = taskWithAmount(BigInteger.valueOf(1_000L))

        suppose("another auto-invest task for the same user and campaign is inserted into database") {
            val numModified = autoInvestTaskRepository.createOrUpdate(newTask)
            assertThat(numModified).isOne()
        }

        verify("auto-invest task is correctly updated") {
            val databaseTask = autoInvestTaskRepository.findById(task.uuid)
            assertThat(databaseTask).hasValue(
                task.copy(
                    amount = newTask.amount,
                    createdAt = newTask.createdAt
                )
            )
        }
    }

    @Test
    fun mustNotUpdateExistingAutoInvestTaskWhenThereIsAStatusMismatch() {
        val task = taskWithAmount(BigInteger.valueOf(500L), status = AutoInvestTaskStatus.IN_PROCESS)

        suppose("auto-invest task is inserted into database") {
            val numModified = autoInvestTaskRepository.createOrUpdate(task)
            assertThat(numModified).isOne()
        }

        val newTask = taskWithAmount(BigInteger.valueOf(1_000), status = AutoInvestTaskStatus.PENDING)

        suppose("another auto-invest task for the same user and campaign is inserted into database") {
            val numModified = autoInvestTaskRepository.createOrUpdate(newTask)
            assertThat(numModified).isZero()
        }

        verify("auto-invest task is not updated") {
            val databaseTask = autoInvestTaskRepository.findById(task.uuid)
            assertThat(databaseTask).hasValue(task)
        }
    }

    @Test
    fun mustCorrectlyFetchAutoInvestTasksByStatus() {
        val targetTasks = listOf(
            taskForUser("user1", chainId = 2L, status = AutoInvestTaskStatus.IN_PROCESS),
            taskForUser("user2", chainId = 2L, status = AutoInvestTaskStatus.IN_PROCESS),
            taskForUser("user3", chainId = 2L, status = AutoInvestTaskStatus.IN_PROCESS),
            taskForUser("user4", chainId = 2L, status = AutoInvestTaskStatus.IN_PROCESS),
            taskForUser("user5", chainId = 2L, status = AutoInvestTaskStatus.IN_PROCESS),
        )
        val otherTasks = listOf(
            taskForUser("user6"),
            taskForUser("user7"),
            taskForUser("user8"),
            taskForUser("user9"),
            taskForUser("user10"),
        )

        suppose("some auto-invest tasks are in the database") {
            autoInvestTaskRepository.saveAllAndFlush(targetTasks + otherTasks)
        }

        verify("correct auto-invest tasks are returned") {
            val databaseTasks = autoInvestTaskRepository.findByStatus(AutoInvestTaskStatus.IN_PROCESS)
            assertThat(databaseTasks).containsExactlyInAnyOrderElementsOf(targetTasks)
        }
    }

    @Test
    fun mustCorrectlyUpdateStatusForSpecifiedIds() {
        val updatedTasks = listOf(
            taskForUser("user1"),
            taskForUser("user2"),
            taskForUser("user3"),
            taskForUser("user4"),
            taskForUser("user5"),
        )
        val nonUpdatedTasks = listOf(
            taskForUser("user6"),
            taskForUser("user7"),
            taskForUser("user8"),
            taskForUser("user9"),
            taskForUser("user10"),
        )

        suppose("some auto-invest tasks are in the database") {
            autoInvestTaskRepository.saveAllAndFlush(updatedTasks + nonUpdatedTasks)
        }

        suppose("status is updated for specified task IDs") {
            autoInvestTaskRepository.updateStatusForIds(updatedTasks.map { it.uuid }, AutoInvestTaskStatus.IN_PROCESS)
        }

        verify("correct auto-invest tasks are updated") {
            val databaseTasks = autoInvestTaskRepository.findByStatus(AutoInvestTaskStatus.IN_PROCESS)
            assertThat(databaseTasks).containsExactlyInAnyOrderElementsOf(
                updatedTasks.map { it.copy(status = AutoInvestTaskStatus.IN_PROCESS) }
            )
        }
    }

    @Test
    fun mustCorrectlyUpdateStatusAndHashForSpecifiedIds() {
        val updatedTasks = listOf(
            taskForUser("user1"),
            taskForUser("user2"),
            taskForUser("user3"),
            taskForUser("user4"),
            taskForUser("user5"),
        )
        val nonUpdatedTasks = listOf(
            taskForUser("user6"),
            taskForUser("user7"),
            taskForUser("user8"),
            taskForUser("user9"),
            taskForUser("user10"),
        )

        suppose("some auto-invest tasks are in the database") {
            autoInvestTaskRepository.saveAllAndFlush(updatedTasks + nonUpdatedTasks)
        }

        suppose("status and hash is updated for specified task IDs") {
            autoInvestTaskRepository.updateStatusAndHashForIds(
                updatedTasks.map { it.uuid },
                AutoInvestTaskStatus.IN_PROCESS,
                "testHash"
            )
        }

        verify("correct auto-invest tasks are updated") {
            val databaseTasks = autoInvestTaskRepository.findByStatus(AutoInvestTaskStatus.IN_PROCESS)
            assertThat(databaseTasks).containsExactlyInAnyOrderElementsOf(
                updatedTasks.map {
                    it.copy(
                        status = AutoInvestTaskStatus.IN_PROCESS,
                        hash = "testHash"
                    )
                }
            )
        }
    }

    @Test
    fun mustCorrectlyFetchAutoInvestTasksByUserWalletAddressAndCampaignContractAddressAndChainId() {
        val task1 = taskForUser("user1", campaign = "campaign1", chainId = 1L)
        val task2 = taskForUser("user2", campaign = "campaign2", chainId = 2L)
        val tasks = listOf(
            task1,
            task2,
            taskForUser("user3", campaign = "campaign3", chainId = 3L),
            taskForUser("user4", campaign = "campaign4", chainId = 4L),
            taskForUser("user5", campaign = "campaign5", chainId = 5L),
        )

        suppose("some auto-invest tasks are in the database") {
            autoInvestTaskRepository.saveAllAndFlush(tasks)
        }

        verify("correct auto-invest task is returned") {
            val databaseTask1 = autoInvestTaskRepository.findByUserWalletAddressAndCampaignContractAddressAndChainId(
                userWalletAddress = "user1WalletAddress",
                campaignContractAddress = "campaign1ContractAddress",
                chainId = 1L
            )
            assertThat(databaseTask1).isEqualTo(task1)

            val databaseTask2 = autoInvestTaskRepository.findByUserWalletAddressAndCampaignContractAddressAndChainId(
                userWalletAddress = "user2WalletAddress",
                campaignContractAddress = "campaign2ContractAddress",
                chainId = 2L
            )
            assertThat(databaseTask2).isEqualTo(task2)
        }
    }

    @Test
    fun mustCorrectlyMoveTaskToHistoricalTable() {
        val task = taskWithAmount(BigInteger.valueOf(1L))

        suppose("auto-invest task is inserted into database") {
            val numModified = autoInvestTaskRepository.createOrUpdate(task)
            assertThat(numModified).isOne()
        }

        suppose("task is marked as successful") {
            autoInvestTaskRepository.completeTasks(
                listOf(task.uuid),
                AutoInvestTaskHistoryStatus.SUCCESS,
                ZonedDateTime.now()
            )
        }

        verify("auto-invest task was correctly moved to historical table") {
            val databaseTask = autoInvestTaskRepository.findById(task.uuid)
            assertThat(databaseTask).isEmpty()

            val historicalUuids = autoInvestTaskRepository.getHistoricalUuidsForStatus(
                AutoInvestTaskHistoryStatus.SUCCESS
            )
            assertThat(historicalUuids).containsExactly(task.uuid)
        }
    }

    private fun taskForUser(
        user: String,
        campaign: String = "campaign",
        chainId: Long = 1L,
        status: AutoInvestTaskStatus = AutoInvestTaskStatus.PENDING
    ) = AutoInvestTask(
        UUID.randomUUID(),
        chainId,
        "${user}WalletAddress",
        "${campaign}ContractAddress",
        BigInteger.valueOf(1L),
        status,
        null,
        ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
    )

    private fun taskWithAmount(amount: BigInteger, status: AutoInvestTaskStatus = AutoInvestTaskStatus.PENDING) =
        AutoInvestTask(
            UUID.randomUUID(),
            1L,
            "userWalletAddress",
            "campaignContractAddress",
            amount,
            status,
            null,
            ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
        )
}
