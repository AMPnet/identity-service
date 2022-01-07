package com.ampnet.identityservice.controller

import com.ampnet.identityservice.blockchain.properties.Chain
import com.ampnet.identityservice.controller.pojo.request.AutoInvestRequest
import com.ampnet.identityservice.controller.pojo.response.AutoInvestListResponse
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
    private val address = "0xef678007d18427e6022059dbc264f27507cd1ffc"
    private val campaignAddress = "campaignAddress".lowercase()
    private val autoInvestPath = "/auto_invest/$defaultChainId/"

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
                post(autoInvestPath + campaignAddress)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andReturn()

            autoInvestResponse = objectMapper.readValue(result.response.contentAsString)
        }

        verify("Correct auto-invest response is returned") {
            assertThat(autoInvestResponse.walletAddress).isEqualTo(address)
            assertThat(autoInvestResponse.campaignAddress).isEqualTo(campaignAddress)
            assertThat(autoInvestResponse.amount).isEqualTo(BigInteger.valueOf(500L))
        }

        verify("Task is correctly stored into the database") {
            val task = autoInvestTaskRepository.findByUserWalletAddressAndCampaignContractAddressAndChainId(
                userWalletAddress = address,
                campaignContractAddress = campaignAddress,
                chainId = defaultChainId
            )!!

            assertThat(task.userWalletAddress).isEqualTo(address)
            assertThat(task.campaignContractAddress).isEqualTo(campaignAddress)
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
                    address,
                    campaignAddress,
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
                post(autoInvestPath + campaignAddress)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andReturn()

            autoInvestResponse = objectMapper.readValue(result.response.contentAsString)
        }

        verify("Correct auto-invest response is returned") {
            assertThat(autoInvestResponse.walletAddress).isEqualTo(address)
            assertThat(autoInvestResponse.campaignAddress).isEqualTo(campaignAddress)
            assertThat(autoInvestResponse.amount).isEqualTo(BigInteger.valueOf(500L))
        }

        verify("Task is correctly updated in the database") {
            val task = autoInvestTaskRepository.findByChainIdAndUserWalletAddress(
                userWalletAddress = address,
                chainId = defaultChainId
            ).first()

            assertThat(task.userWalletAddress).isEqualTo(address)
            assertThat(task.campaignContractAddress).isEqualTo(campaignAddress)
            assertThat(task.chainId).isEqualTo(defaultChainId)
            assertThat(task.amount).isEqualTo(BigInteger.valueOf(500L))
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
                    address,
                    campaignAddress,
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
                post(autoInvestPath + campaignAddress)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andReturn()
        }

        verify("Task is not updated in the database") {
            val task = autoInvestTaskRepository.findByChainIdAndUserWalletAddress(
                userWalletAddress = address,
                chainId = defaultChainId
            ).first()

            assertThat(task.userWalletAddress).isEqualTo(address)
            assertThat(task.campaignContractAddress).isEqualTo(campaignAddress)
            assertThat(task.chainId).isEqualTo(defaultChainId)
            assertThat(task.amount).isEqualTo(BigInteger.valueOf(1000L))
            assertThat(task.status).isEqualTo(AutoInvestTaskStatus.IN_PROCESS)
        }
    }

    @Test
    fun mustCorrectlyReturnAutoInvestTask() {
        suppose("Auto-invest task exists for user") {
            autoInvestTaskRepository.createOrUpdate(
                AutoInvestTask(
                    UUID.randomUUID(),
                    defaultChainId,
                    address,
                    campaignAddress,
                    BigInteger.valueOf(1000L),
                    AutoInvestTaskStatus.PENDING,
                    null,
                    ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
                )
            )
        }

        lateinit var autoInvestListResponse: AutoInvestListResponse

        suppose("User requests auto-invest task") {
            val result = mockMvc.perform(get(autoInvestPath + address))
                .andExpect(status().isOk)
                .andReturn()

            autoInvestListResponse = objectMapper.readValue(result.response.contentAsString)
        }

        verify("Correct auto-invest response is returned") {
            assertThat(autoInvestListResponse.autoInvests).hasSize(1)

            val autoInvestResponse = autoInvestListResponse.autoInvests.first()
            assertThat(autoInvestResponse.walletAddress).isEqualTo(address)
            assertThat(autoInvestResponse.campaignAddress).isEqualTo(campaignAddress)
            assertThat(autoInvestResponse.amount).isEqualTo(BigInteger.valueOf(1000L))
        }
    }

    @Test
    fun mustReturnMultipleTasksForDifferentCampaigns() {
        lateinit var autoInvestListResponse: AutoInvestListResponse
        suppose("Auto-invest tasks exist for user") {
            autoInvestTaskRepository.createOrUpdate(
                AutoInvestTask(
                    UUID.randomUUID(),
                    defaultChainId,
                    address,
                    campaignAddress,
                    BigInteger.valueOf(1000L),
                    AutoInvestTaskStatus.PENDING,
                    null,
                    ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
                )
            )
            autoInvestTaskRepository.createOrUpdate(
                AutoInvestTask(
                    UUID.randomUUID(),
                    defaultChainId,
                    address,
                    "campaignaddress-2",
                    BigInteger.valueOf(1000L),
                    AutoInvestTaskStatus.PENDING,
                    null,
                    ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
                )
            )
        }

        suppose("User requests auto-invest task") {
            val result = mockMvc.perform(get(autoInvestPath + address))
                .andExpect(status().isOk)
                .andReturn()

            autoInvestListResponse = objectMapper.readValue(result.response.contentAsString)
        }

        verify("Correct auto-invest response is returned") {
            assertThat(autoInvestListResponse.autoInvests).hasSize(2)
            assertThat(autoInvestListResponse.autoInvests.map { it.walletAddress })
                .containsOnly("0xef678007d18427e6022059dbc264f27507cd1ffc")
            assertThat(autoInvestListResponse.autoInvests.map { it.campaignAddress })
                .containsExactlyInAnyOrderElementsOf(listOf(campaignAddress, "campaignaddress-2"))
            assertThat(autoInvestListResponse.autoInvests.map { it.amount })
                .containsOnly(BigInteger.valueOf(1000L))
        }
    }
}
