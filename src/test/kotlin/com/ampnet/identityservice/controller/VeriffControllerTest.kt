package com.ampnet.identityservice.controller

import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.persistence.model.VeriffDecision
import com.ampnet.identityservice.persistence.model.VeriffSession
import com.ampnet.identityservice.persistence.model.VeriffSessionState
import com.ampnet.identityservice.security.WithMockCrowdfundUser
import com.ampnet.identityservice.service.pojo.ServiceVerificationResponse
import com.ampnet.identityservice.service.pojo.VeriffStatus
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.security.MessageDigest

class VeriffControllerTest : ControllerTestBase() {

    private val veriffPath = "/veriff"
    private val xClientHeader = "X-AUTH-CLIENT"
    private val xSignature = "X-SIGNATURE"

    private lateinit var testContext: TestContext
    private lateinit var mockServer: MockRestServiceServer

    @BeforeEach
    fun init() {
        testContext = TestContext()
        mockServer = MockRestServiceServer.createServer(restTemplate)
    }

    @Test
    @WithMockCrowdfundUser
    fun mustReturnVeriffSession() {
        suppose("User has an account") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllUserInfos()
            databaseCleanerService.deleteAllVeriffSessions()
            testContext.user = createUser()
            val veriffSession = VeriffSession(
                "44927492-8799-406e-8076-933bc9164ebc",
                testContext.user.address,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.address,
                "https://alchemy.veriff.com/",
                "created",
                false,
                zonedDateTimeProvider.getZonedDateTime(),
                VeriffSessionState.SUBMITTED
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff posted declined decision") {
            databaseCleanerService.deleteAllVeriffDecisions()
            val decision = VeriffDecision(
                testContext.veriffSession.id,
                VeriffStatus.declined,
                9102,
                "Physical document not used",
                101,
                "2020-12-04T10:45:37.907Z",
                "2020-12-04T10:45:31.000Z",
                zonedDateTimeProvider.getZonedDateTime()
            )
            veriffDecisionRepository.save(decision)
        }
        suppose("Veriff will return new session") {
            val response = getResourceAsText("/veriff/response-new-session.json")
            mockVeriffResponse(response, HttpMethod.POST, "/v1/sessions/")
        }

        verify("Controller will return new veriff session") {
            val result = mockMvc.perform(
                post("/veriff/session")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            val veriffResponse: ServiceVerificationResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(veriffResponse.decision).isNotNull
            assertThat(veriffResponse.verificationUrl)
                .isEqualTo("https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.new-url")
        }
    }

    @Test
    fun mustStoreUserInfoFromVeriff() {
        suppose("User has no user info") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllUserInfos()
            testContext.user = createUser()
        }

        verify("Controller will accept valid data") {
            val request = getResourceAsText("/veriff/response-with-vendor-data.json")
            mockMvc.perform(
                post("$veriffPath/webhook/decision")
                    .content(request)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(xClientHeader, applicationProperties.veriff.apiKey)
                    .header(xSignature, generateSignature(request))
            )
                .andExpect(status().isOk)
        }
        verify("User info is stored") {
            val userInfoList = userInfoRepository
                .findBySessionIdOrderByCreatedAtDesc("12df6045-3846-3e45-946a-14fa6136d78b")
            assertThat(userInfoList.first().connected).isTrue()
        }
    }

    @Test
    fun mustHandleVeriffWebhookEvent() {
        suppose("User has no user info") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllUserInfos()
            databaseCleanerService.deleteAllVeriffSessions()
            testContext.user = createUser()
        }
        suppose("User has veriff session") {
            val veriffSession = VeriffSession(
                "cbb238c6-51a0-482b-bd1a-42a2e0b0ff1c",
                testContext.user.address,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.address,
                "https://alchemy.veriff.com/",
                "created",
                false,
                zonedDateTimeProvider.getZonedDateTime(),
                VeriffSessionState.CREATED
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }

        verify("Controller will accept submitted event data") {
            val request = getResourceAsText("/veriff/response-event-submitted.json")
            mockMvc.perform(
                post("$veriffPath/webhook/event")
                    .content(request)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(xClientHeader, applicationProperties.veriff.apiKey)
                    .header(xSignature, generateSignature(request))
            )
                .andExpect(status().isOk)
        }
        verify("Veriff session is updated") {
            val veriffSession = veriffSessionRepository.findById(testContext.veriffSession.id)
            assertThat(veriffSession.isPresent)
            assertThat(veriffSession.get().state).isEqualTo(VeriffSessionState.SUBMITTED)
        }
    }

    @Test
    fun mustReturnBadRequestForEventInvalidSignature() {
        verify("Controller will return bad request for invalid signature header data") {
            val request = getResourceAsText("/veriff/response-event-submitted.json")
            mockMvc.perform(
                post("$veriffPath/webhook/event")
                    .content(request)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(xClientHeader, applicationProperties.veriff.apiKey)
                    .header(xSignature, "invalid-signature")
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun mustReturnBadRequestForEventInvalidClient() {
        verify("Controller will return bad request for invalid client header data") {
            val request = getResourceAsText("/veriff/response-event-submitted.json")
            mockMvc.perform(
                post("$veriffPath/webhook/event")
                    .content(request)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(xClientHeader, "invalid-api-key")
                    .header(xSignature, "bf3da6e9aa47e6be208fec283097a5bcbdb2066dcb58f0d7c9879637700f013f")
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun mustReturnBadRequestForDecisionInvalidSignature() {
        verify("Controller will return bad request for invalid signature header data") {
            val request = getResourceAsText("/veriff/response-with-vendor-data.json")
            mockMvc.perform(
                post("$veriffPath/webhook/decision")
                    .content(request)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(xClientHeader, applicationProperties.veriff.apiKey)
                    .header(xSignature, "invalid-signature")
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun mustReturnBadRequestForDecisionInvalidClient() {
        verify("Controller will return bad request for invalid client header data") {
            val request = getResourceAsText("/veriff/response-with-vendor-data.json")
            mockMvc.perform(
                post("$veriffPath/webhook/decision")
                    .content(request)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(xClientHeader, "invalid-api-key")
                    .header(xSignature, "0b65acb46ddb2a881f5adf742c03b81290ec783db3ef425d13a2c2448f400f64")
            )
                .andExpect(status().isBadRequest)
        }
    }

    private fun mockVeriffResponse(body: String, method: HttpMethod, path: String) {
        val status = MockRestResponseCreators.withStatus(HttpStatus.OK)
        mockServer.expect(
            ExpectedCount.once(),
            MockRestRequestMatchers.requestTo(applicationProperties.veriff.baseUrl + path)
        )
            .andExpect(MockRestRequestMatchers.method(method))
            .andRespond(status.body(body).contentType(MediaType.APPLICATION_JSON))
    }

    private fun generateSignature(payload: String): String {
        val request = payload + applicationProperties.veriff.privateKey
        val hash = MessageDigest.getInstance("SHA-256").digest(request.toByteArray())
        return bytesToHex(hash)
    }

    private fun bytesToHex(hash: ByteArray): String {
        val hexString = StringBuilder(2 * hash.size)
        for (i in hash.indices) {
            val hex = Integer.toHexString(0xff and hash[i].toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }

    private class TestContext {
        lateinit var user: User
        lateinit var veriffSession: VeriffSession
    }
}
