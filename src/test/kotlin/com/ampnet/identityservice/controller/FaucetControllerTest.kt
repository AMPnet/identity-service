package com.ampnet.identityservice.controller

import com.ampnet.identityservice.blockchain.properties.Chain
import com.ampnet.identityservice.controller.pojo.request.ReCaptchaRequest
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.ReCaptchaException
import com.ampnet.identityservice.persistence.model.FaucetTaskStatus
import com.ampnet.identityservice.persistence.repository.FaucetTaskRepository
import com.ampnet.identityservice.service.ReCaptchaService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class FaucetControllerTest : ControllerTestBase() {

    private val defaultChainId = Chain.MATIC_TESTNET_MUMBAI.id
    private val address = "0xef678007d18427e6022059dbc264f27507cd1ffc"
    private val faucetPath = "/faucet/$defaultChainId/$address"

    @Autowired
    private lateinit var faucetTaskRepository: FaucetTaskRepository

    @MockBean
    private lateinit var reCaptchaService: ReCaptchaService

    @BeforeEach
    fun beforeEach() {
        databaseCleanerService.deleteAllFaucetTasks()
        databaseCleanerService.deleteAllQueuedFaucetAddresses()
    }

    @AfterEach
    fun afterEach() {
        databaseCleanerService.deleteAllFaucetTasks()
        databaseCleanerService.deleteAllQueuedFaucetAddresses()
    }

    @Test
    fun mustBeAbleToRequestFaucetFunds() {
        val reCaptchaToken = "token"
        suppose("ReCAPTCHA verification is successful") {
            given(reCaptchaService.validateResponseToken(reCaptchaToken)).willAnswer { }
        }
        suppose("User requests faucet funds") {
            val request = objectMapper.writeValueAsString(ReCaptchaRequest(reCaptchaToken))
            mockMvc.perform(
                post(faucetPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
                .andExpect(status().isOk)
        }

        val taskUuid = UUID.randomUUID()
        val now = ZonedDateTime.now()

        suppose("Address queue is flushed for matic testnet") {
            faucetTaskRepository.flushAddressQueueForChainId(taskUuid, defaultChainId, now, 100)
        }

        verify("Task is created for flushed addresses for matic testnet") {
            val task = faucetTaskRepository.findById(taskUuid)
            assertThat(task).hasValueSatisfying {
                assertThat(it.addresses).containsExactly(address)
                assertThat(it.chainId).isEqualTo(defaultChainId)
                assertThat(it.status).isEqualTo(FaucetTaskStatus.CREATED)
            }
        }
    }

    @Test
    fun mustFailForInvalidReCaptchaTokenRequestFaucetFunds() {
        val reCaptchaToken = "token"
        suppose("ReCAPTCHA verification has failed") {
            given(reCaptchaService.validateResponseToken(reCaptchaToken))
                .willAnswer { throw ReCaptchaException("ReCAPTCHA verification failed") }
        }

        verify("Controller will reject invalid ReCaptcha token") {
            val request = objectMapper.writeValueAsString(ReCaptchaRequest(reCaptchaToken))
            val response = mockMvc.perform(
                post(faucetPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(response, ErrorCode.REG_RECAPTCHA)
        }
    }

    @Test
    fun mustReturnErrorWhenFaucetIsNotSupportedForChainId() {
        verify("Faucet request fails for unsupported chain") {
            mockMvc.perform(
                post("/faucet/0/$address")
            )
                .andExpect(status().isBadRequest)
        }
    }
}
