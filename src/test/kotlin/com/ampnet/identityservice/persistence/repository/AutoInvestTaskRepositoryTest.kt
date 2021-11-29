package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.persistence.model.AutoInvestTask
import com.ampnet.identityservice.persistence.model.AutoInvestTaskStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@DataJpaTest
@ExtendWith(value = [SpringExtension::class])
@AutoConfigureTestDatabase
class AutoInvestTaskRepositoryTest : TestBase() {

    @Autowired
    private lateinit var blockchainTaskRepository: AutoInvestTaskRepository

    @Test
    fun mustCorrectlyInsertAutoInvestTask() {
        val task = taskWithAmount(BigDecimal("12345.123456789"))

        suppose("auto-invest task is inserted into database") {
            val numModified = blockchainTaskRepository.createOrUpdate(task)
            assertThat(numModified).isOne()
        }

        verify("auto-invest task was correctly inserted into database") {
            val databaseTask = blockchainTaskRepository.findById(task.uuid)
            assertThat(databaseTask).hasValue(task)
        }
    }

    @Test
    fun mustCorrectlyUpdateExistingAutoInvestTask() {
        val task = taskWithAmount(BigDecimal(500))

        suppose("auto-invest task is inserted into database") {
            val numModified = blockchainTaskRepository.createOrUpdate(task)
            assertThat(numModified).isOne()
        }

        val newTask = taskWithAmount(BigDecimal(1_000))

        suppose("another auto-invest task for the same user and campaign is inserted into database") {
            val numModified = blockchainTaskRepository.createOrUpdate(newTask)
            assertThat(numModified).isOne()
        }

        verify("auto-invest task is correctly updated") {
            val databaseTask = blockchainTaskRepository.findById(task.uuid)
            assertThat(databaseTask).hasValue(
                task.copy(
                    amount = task.amount + newTask.amount,
                    createdAt = newTask.createdAt
                )
            )
        }
    }

    @Test
    fun mustNotUpdateExistingAutoInvestTaskWhenThereIsAStatusMismatch() {
        val task = taskWithAmount(BigDecimal(500), status = AutoInvestTaskStatus.IN_PROCESS)

        suppose("auto-invest task is inserted into database") {
            val numModified = blockchainTaskRepository.createOrUpdate(task)
            assertThat(numModified).isOne()
        }

        val newTask = taskWithAmount(BigDecimal(1_000), status = AutoInvestTaskStatus.PENDING)

        suppose("another auto-invest task for the same user and campaign is inserted into database") {
            val numModified = blockchainTaskRepository.createOrUpdate(newTask)
            assertThat(numModified).isZero()
        }

        verify("auto-invest task is not updated") {
            val databaseTask = blockchainTaskRepository.findById(task.uuid)
            assertThat(databaseTask).hasValue(task)
        }
    }

    @Test
    fun mustCorrectlyFetchAutoInvestTasksByChainIdAndStatus() {
        val targetTasks = listOf(
            taskForUser("user1", chainId = 2L, status = AutoInvestTaskStatus.IN_PROCESS),
            taskForUser("user2", chainId = 2L, status = AutoInvestTaskStatus.IN_PROCESS),
            taskForUser("user3", chainId = 2L, status = AutoInvestTaskStatus.IN_PROCESS),
            taskForUser("user4", chainId = 2L, status = AutoInvestTaskStatus.IN_PROCESS),
            taskForUser("user5", chainId = 2L, status = AutoInvestTaskStatus.IN_PROCESS),
        )
        val otherTasks = listOf(
            taskForUser("user6"),
            taskForUser("user7", chainId = 2L),
            taskForUser("user8", status = AutoInvestTaskStatus.IN_PROCESS),
            taskForUser("user9"),
            taskForUser("user10"),
        )

        suppose("some auto-invest tasks are in the database") {
            blockchainTaskRepository.saveAllAndFlush(targetTasks + otherTasks)
        }

        verify("correct auto-invest tasks are returned") {
            val databaseTasks = blockchainTaskRepository.findByChainIdAndStatus(2L, AutoInvestTaskStatus.IN_PROCESS)
            assertThat(databaseTasks).containsExactlyInAnyOrderElementsOf(targetTasks)
        }
    }

    private fun taskForUser(
        user: String,
        chainId: Long = 1L,
        status: AutoInvestTaskStatus = AutoInvestTaskStatus.PENDING
    ) = AutoInvestTask(
        UUID.randomUUID(),
        chainId,
        "${user}WalletAddress",
        "campaignContractAddress",
        BigDecimal(1L),
        status,
        ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
    )

    private fun taskWithAmount(amount: BigDecimal, status: AutoInvestTaskStatus = AutoInvestTaskStatus.PENDING) =
        AutoInvestTask(
            UUID.randomUUID(),
            1L,
            "userWalletAddress",
            "campaignContractAddress",
            amount,
            status,
            ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
        )
}
