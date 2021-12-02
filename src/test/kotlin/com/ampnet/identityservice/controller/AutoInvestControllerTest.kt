package com.ampnet.identityservice.controller

import com.ampnet.identityservice.blockchain.properties.Chain
import com.ampnet.identityservice.controller.pojo.request.AutoInvestRequest
import com.ampnet.identityservice.controller.pojo.response.AutoInvestResponse
import com.ampnet.identityservice.persistence.model.AutoInvestTask
import com.ampnet.identityservice.persistence.model.AutoInvestTaskStatus
import com.ampnet.identityservice.persistence.repository.AutoInvestTaskRepository
import com.ampnet.identityservice.security.WithMockCrowdfundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigInteger
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class AutoInvestControllerTest : ControllerTestBase() {

    private val defaultChainId = Chain.MATIC_TESTNET_MUMBAI.id
    private val autoInvestPath = "/auto_invest/$defaultChainId/campaignAddress"

    @Autowired
    private lateinit var autoInvestTaskRepository: AutoInvestTaskRepository

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllUsers()
        databaseCleanerService.deleteAllAutoInvestTasks()
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToRequestAutoInvest() {
        val request = AutoInvestRequest(amount = BigInteger.valueOf(500L))
        lateinit var autoInvestResponse: AutoInvestResponse

        suppose("User submits auto-invest request") {
            val result = mockMvc.perform(
                post(autoInvestPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andReturn()

            autoInvestResponse = objectMapper.readValue(result.response.contentAsString)
        }

        verify("Correct auto-invest response is returned") {
            assertThat(autoInvestResponse.walletAddress).isEqualTo("0xef678007d18427e6022059dbc264f27507cd1ffc")
            assertThat(autoInvestResponse.campaignAddress).isEqualTo("campaignAddress")
            assertThat(autoInvestResponse.amount).isEqualTo(BigInteger.valueOf(500L))
        }

        verify("Task is correctly stored into the database") {
            val task = autoInvestTaskRepository.findByUserWalletAddressAndCampaignContractAddressAndChainId(
                userWalletAddress = "0xef678007d18427e6022059dbc264f27507cd1ffc",
                campaignContractAddress = "campaignAddress",
                chainId = defaultChainId
            )!!

            assertThat(task.userWalletAddress).isEqualTo("0xef678007d18427e6022059dbc264f27507cd1ffc")
            assertThat(task.campaignContractAddress).isEqualTo("campaignAddress")
            assertThat(task.chainId).isEqualTo(defaultChainId)
            assertThat(task.amount).isEqualTo(BigInteger.valueOf(500L))
            assertThat(task.status).isEqualTo(AutoInvestTaskStatus.PENDING)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToUpdateAutoInvestTaskWhenNotInProcess() {
        suppose("Pending auto-invest task already exists for user") {
            autoInvestTaskRepository.createOrUpdate(
                AutoInvestTask(
                    UUID.randomUUID(),
                    defaultChainId,
                    "0xef678007d18427e6022059dbc264f27507cd1ffc",
                    "campaignAddress",
                    BigInteger.valueOf(1000L),
                    AutoInvestTaskStatus.PENDING,
                    null,
                    ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
                )
            )
        }

        val request = AutoInvestRequest(amount = BigInteger.valueOf(500L))
        lateinit var autoInvestResponse: AutoInvestResponse

        suppose("User submits auto-invest request") {
            val result = mockMvc.perform(
                post(autoInvestPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andReturn()

            autoInvestResponse = objectMapper.readValue(result.response.contentAsString)
        }

        verify("Correct auto-invest response is returned") {
            assertThat(autoInvestResponse.walletAddress).isEqualTo("0xef678007d18427e6022059dbc264f27507cd1ffc")
            assertThat(autoInvestResponse.campaignAddress).isEqualTo("campaignAddress")
            assertThat(autoInvestResponse.amount).isEqualTo(BigInteger.valueOf(1500L))
        }

        verify("Task is correctly updated in the database") {
            val task = autoInvestTaskRepository.findByUserWalletAddressAndCampaignContractAddressAndChainId(
                userWalletAddress = "0xef678007d18427e6022059dbc264f27507cd1ffc",
                campaignContractAddress = "campaignAddress",
                chainId = defaultChainId
            )!!

            assertThat(task.userWalletAddress).isEqualTo("0xef678007d18427e6022059dbc264f27507cd1ffc")
            assertThat(task.campaignContractAddress).isEqualTo("campaignAddress")
            assertThat(task.chainId).isEqualTo(defaultChainId)
            assertThat(task.amount).isEqualTo(BigInteger.valueOf(1500L))
            assertThat(task.status).isEqualTo(AutoInvestTaskStatus.PENDING)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustNotUpdateAutoInvestTaskWhenAlreadyInProcess() {
        suppose("In process auto-invest task already exists for user") {
            autoInvestTaskRepository.createOrUpdate(
                AutoInvestTask(
                    UUID.randomUUID(),
                    defaultChainId,
                    "0xef678007d18427e6022059dbc264f27507cd1ffc",
                    "campaignAddress",
                    BigInteger.valueOf(1000L),
                    AutoInvestTaskStatus.IN_PROCESS,
                    null,
                    ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
                )
            )
        }

        val request = AutoInvestRequest(amount = BigInteger.valueOf(500L))

        suppose("User submits auto-invest request") {
            mockMvc.perform(
                post(autoInvestPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andReturn()
        }

        verify("Task is not updated in the database") {
            val task = autoInvestTaskRepository.findByUserWalletAddressAndCampaignContractAddressAndChainId(
                userWalletAddress = "0xef678007d18427e6022059dbc264f27507cd1ffc",
                campaignContractAddress = "campaignAddress",
                chainId = defaultChainId
            )!!

            assertThat(task.userWalletAddress).isEqualTo("0xef678007d18427e6022059dbc264f27507cd1ffc")
            assertThat(task.campaignContractAddress).isEqualTo("campaignAddress")
            assertThat(task.chainId).isEqualTo(defaultChainId)
            assertThat(task.amount).isEqualTo(BigInteger.valueOf(1000L))
            assertThat(task.status).isEqualTo(AutoInvestTaskStatus.IN_PROCESS)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustCorrectlyReturnAutoInvestTask() {
        suppose("Auto-invest task exists for user") {
            autoInvestTaskRepository.createOrUpdate(
                AutoInvestTask(
                    UUID.randomUUID(),
                    defaultChainId,
                    "0xef678007d18427e6022059dbc264f27507cd1ffc",
                    "campaignAddress",
                    BigInteger.valueOf(1000L),
                    AutoInvestTaskStatus.PENDING,
                    null,
                    ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
                )
            )
        }

        lateinit var autoInvestResponse: AutoInvestResponse

        suppose("User requests auto-invest task") {
            val result = mockMvc.perform(get(autoInvestPath))
                .andExpect(status().isOk)
                .andReturn()

            autoInvestResponse = objectMapper.readValue(result.response.contentAsString)
        }

        verify("Correct auto-invest response is returned") {
            assertThat(autoInvestResponse.walletAddress).isEqualTo("0xef678007d18427e6022059dbc264f27507cd1ffc")
            assertThat(autoInvestResponse.campaignAddress).isEqualTo("campaignAddress")
            assertThat(autoInvestResponse.amount).isEqualTo(BigInteger.valueOf(1000L))
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustReturnZeroAmountForNonExistentAutoInvestTask() {
        lateinit var autoInvestResponse: AutoInvestResponse

        suppose("User requests auto-invest task") {
            val result = mockMvc.perform(get(autoInvestPath))
                .andExpect(status().isOk)
                .andReturn()

            autoInvestResponse = objectMapper.readValue(result.response.contentAsString)
        }

        verify("Correct auto-invest response is returned") {
            assertThat(autoInvestResponse.walletAddress).isEqualTo("0xef678007d18427e6022059dbc264f27507cd1ffc")
            assertThat(autoInvestResponse.campaignAddress).isEqualTo("campaignAddress")
            assertThat(autoInvestResponse.amount).isEqualTo(BigInteger.valueOf(0L))
        }
    }
}
