package com.ampnet.identityservice.service

import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.ResourceNotFoundException
import com.ampnet.identityservice.exception.VeriffException
import com.ampnet.identityservice.exception.VeriffReasonCode
import com.ampnet.identityservice.exception.VeriffVerificationCode
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.persistence.model.UserInfo
import com.ampnet.identityservice.persistence.model.VeriffDecision
import com.ampnet.identityservice.persistence.model.VeriffSession
import com.ampnet.identityservice.persistence.model.VeriffSessionState
import com.ampnet.identityservice.service.impl.UserServiceImpl
import com.ampnet.identityservice.service.impl.VeriffServiceImpl
import com.ampnet.identityservice.service.pojo.VeriffResponse
import com.ampnet.identityservice.service.pojo.VeriffStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.mockito.kotlin.mock
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import java.util.Locale

class VeriffServiceTest : JpaServiceTestBase() {

    private lateinit var mockServer: MockRestServiceServer
    private val baseUrl = "http://localhost:8080"
    private val zonedDateTimeProvider = CurrentZonedDateTimeProvider()

    private val veriffService: VeriffServiceImpl by lazy {
        val mailService = mock<MailService>()
        val uuidProvider = RandomUuidProvider()
        val blockchainQueueService = mock<BlockchainQueueService>()
        val userService = UserServiceImpl(
            uuidProvider, zonedDateTimeProvider, userRepository, userInfoRepository, mailTokenRepository,
            mailService, applicationProperties, blockchainQueueService
        )
        VeriffServiceImpl(
            zonedDateTimeProvider, veriffSessionRepository, veriffDecisionRepository, userInfoRepository,
            applicationProperties, userService, restTemplate, camelCaseObjectMapper
        )
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllUsers()
        databaseCleanerService.deleteAllVeriffSessions()
        databaseCleanerService.deleteAllVeriffDecisions()
        testContext = TestContext()
        mockServer = MockRestServiceServer.createServer(restTemplate)
    }

    @Test
    fun mustSaveUserData() {
        suppose("There is unverified user") {
            testContext.user = createUser("12345678")
        }

        verify("Service will store valid user data") {
            val veriffResponse = getResourceAsText("/veriff/response.json")
            testContext.userInfo = veriffService.handleDecision(veriffResponse) ?: fail("Missing user info")
            assertThat(testContext.userInfo.sessionId).isEqualTo("12df6045-3846-3e45-946a-14fa6136d78b")
            assertThat(testContext.userInfo.firstName).isEqualTo("SARAH")
            assertThat(testContext.userInfo.lastName).isEqualTo("MORGAN")
            assertThat(testContext.userInfo.dateOfBirth).isEqualTo("1967-03-30")
            assertThat(testContext.userInfo.placeOfBirth).isEqualTo("MADRID")
            assertThat(testContext.userInfo.document.type).isEqualTo("DRIVERS_LICENSE")
            assertThat(testContext.userInfo.document.number).isEqualTo("MORGA753116SM9IJ")
            assertThat(testContext.userInfo.document.country).isEqualTo("GB")
            assertThat(testContext.userInfo.document.validUntil).isEqualTo("2022-04-20")
        }
        verify("User data is stored") {
            assertThat(userInfoRepository.findById(testContext.userInfo.uuid)).isNotNull
        }
    }

    @Test
    fun mustReturnExistingSessionForApprovedResponse() {
        suppose("User has veriff session") {
            testContext.user = createUser()
            val veriffSession = VeriffSession(
                "12df6045-3846-3e45-946a-14fa6136d78b",
                testContext.user.address,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.address,
                "https://alchemy.veriff.com",
                "created",
                false,
                zonedDateTimeProvider.getZonedDateTime(),
                VeriffSessionState.SUBMITTED
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff decision is approved") {
            veriffPostedDecision("/veriff/response.json")
        }

        verify("Service will return url from stored veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.address, baseUrl)
                ?: fail("Service didn't return session")
            assertThat(response.verificationUrl).isEqualTo(testContext.veriffSession.url)
            val decision = response.decision ?: fail("Missing decision")
            assertThat(decision.sessionId).isEqualTo(testContext.veriffSession.id)
            assertThat(decision.status).isEqualTo(VeriffStatus.approved)
            assertThat(decision.code).isEqualTo(VeriffVerificationCode.POSITIVE.code)
            assertThat(decision.reasonCode).isNull()
            assertThat(decision.reason).isNull()
            assertThat(decision.acceptanceTime).isNotNull
            assertThat(decision.decisionTime).isNotNull
        }
    }

    @Test
    fun mustReturnExistingSessionForResubmissionResponse() {
        suppose("User has veriff session") {
            testContext.user = createUser()
            val veriffSession = VeriffSession(
                "32599b8e-e596-4601-973c-aa197ae0dfde",
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
        suppose("Veriff posted resubmission decision") {
            veriffPostedDecision("/veriff/response-resubmission.json")
        }

        verify("Service will return url from stored veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.address, baseUrl)
                ?: fail("Service didn't return session")
            assertThat(response.verificationUrl).isEqualTo(testContext.veriffSession.url)
            val decision = response.decision ?: fail("Missing decision")
            assertThat(decision.sessionId).isEqualTo(testContext.veriffSession.id)
            assertThat(decision.status).isEqualTo(VeriffStatus.resubmission_requested)
            assertThat(decision.code).isEqualTo(VeriffVerificationCode.RESUBMISSION.code)
            assertThat(decision.reasonCode).isEqualTo(VeriffReasonCode.DOC_NOT_VISIBLE.code)
            assertThat(decision.reason).isNotNull
            assertThat(decision.acceptanceTime).isNotNull
            assertThat(decision.decisionTime).isNotNull
        }
    }

    @Test
    fun mustCreateNewSessionForDeclinedResponse() {
        suppose("User has veriff session") {
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
            veriffPostedDecision("/veriff/response-declined.json")
        }
        suppose("Veriff will return new session") {
            val response = getResourceAsText("/veriff/response-new-session.json")
            mockVeriffResponse(response, HttpMethod.POST, "/v1/sessions/")
        }

        verify("Service will return a new url veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.address, baseUrl)
                ?: fail("Service didn't return session")
            assertThat(response.verificationUrl).isNotEqualTo(testContext.veriffSession.url)
            val decision = response.decision ?: fail("Missing decision")
            assertThat(decision.sessionId).isEqualTo(testContext.veriffSession.id)
            assertThat(decision.status).isEqualTo(VeriffStatus.declined)
            assertThat(decision.code).isEqualTo(VeriffVerificationCode.NEGATIVE.code)
            assertThat(decision.reasonCode).isEqualTo(VeriffReasonCode.DOC_NOT_USED.code)
            assertThat(decision.reason).isNotNull
            assertThat(decision.acceptanceTime).isNotNull
            assertThat(decision.decisionTime).isNotNull
        }
        verify("New veriff session is created") {
            val veriffSessions = veriffSessionRepository.findByUserAddressOrderByCreatedAtDesc(testContext.user.address)
            assertThat(veriffSessions).hasSize(2)
            verifyNewVeriffSession(veriffSessions.first())
        }
    }

    @Test
    fun mustCreateNewSessionForAbandonedResponse() {
        suppose("User has veriff session") {
            testContext.user = createUser()
            val veriffSession = VeriffSession(
                "4c0be76b-d01d-46d5-8c07-a0e2629ebd86",
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
        suppose("Veriff posted abandoned decision") {
            veriffPostedDecision("/veriff/response-abandoned.json")
        }
        suppose("Veriff will return new session") {
            val response = getResourceAsText("/veriff/response-new-session.json")
            mockVeriffResponse(response, HttpMethod.POST, "/v1/sessions/")
        }

        verify("Service will return a new url veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.address, baseUrl)
                ?: fail("Service didn't return session")
            assertThat(response.verificationUrl).isNotEqualTo(testContext.veriffSession.url)
            val decision = response.decision ?: fail("Missing decision")
            assertThat(decision.sessionId).isEqualTo(testContext.veriffSession.id)
            assertThat(decision.status).isEqualTo(VeriffStatus.abandoned)
            assertThat(decision.code).isEqualTo(VeriffVerificationCode.NEGATIVE_EXPIRED.code)
            assertThat(decision.reasonCode).isNull()
            assertThat(decision.reason).isNull()
            assertThat(decision.acceptanceTime).isNotNull
            assertThat(decision.decisionTime).isNull()
        }
        verify("New veriff session is created") {
            val veriffSessions = veriffSessionRepository.findByUserAddressOrderByCreatedAtDesc(testContext.user.address)
            assertThat(veriffSessions).hasSize(2)
            verifyNewVeriffSession(veriffSessions.first())
        }
    }

    @Test
    fun mustCreateNewSessionForExpiredResponse() {
        suppose("User has veriff session") {
            testContext.user = createUser()
            val veriffSession = VeriffSession(
                "eb52789e-1cce-4c26-a86e-d111bb75bd27",
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
        suppose("Veriff posted expired decision") {
            veriffPostedDecision("/veriff/response-expired.json")
        }
        suppose("Veriff will return new session") {
            val response = getResourceAsText("/veriff/response-new-session.json")
            mockVeriffResponse(response, HttpMethod.POST, "/v1/sessions/")
        }

        verify("Service will return a new url veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.address, baseUrl)
                ?: fail("Service didn't return session")
            assertThat(response.verificationUrl).isNotEqualTo(testContext.veriffSession.url)
            val decision = response.decision ?: fail("Missing decision")
            assertThat(decision.sessionId).isEqualTo(testContext.veriffSession.id)
            assertThat(decision.status).isEqualTo(VeriffStatus.expired)
            assertThat(decision.code).isEqualTo(VeriffVerificationCode.NEGATIVE_EXPIRED.code)
            assertThat(decision.reasonCode).isNull()
            assertThat(decision.reason).isNull()
            assertThat(decision.acceptanceTime).isNotNull
            assertThat(decision.decisionTime).isNull()
        }
        verify("New veriff session is created") {
            val veriffSessions = veriffSessionRepository.findByUserAddressOrderByCreatedAtDesc(testContext.user.address)
            assertThat(veriffSessions).hasSize(2)
            verifyNewVeriffSession(veriffSessions.first())
        }
    }

    @Test
    fun mustCreateNewSessionForSessionOlderThan7Days() {
        suppose("User has veriff session") {
            testContext.user = createUser()
            val veriffSession = VeriffSession(
                "eb52789e-1cce-4c26-a86e-d111bb75bd27",
                testContext.user.address,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.address,
                "https://alchemy.veriff.com/",
                "created",
                false,
                zonedDateTimeProvider.getZonedDateTime().minusDays(8),
                VeriffSessionState.CREATED
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff will return new session") {
            val response = getResourceAsText("/veriff/response-new-session.json")
            mockVeriffResponse(response, HttpMethod.POST, "/v1/sessions/")
        }

        verify("Service will return a new url veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.address, baseUrl)
                ?: fail("Service didn't return session")
            assertThat(response.verificationUrl).isNotEqualTo(testContext.veriffSession.url)
            assertThat(response.state).isEqualTo(VeriffSessionState.CREATED.name.lowercase(Locale.getDefault()))
            assertThat(response.decision).isNull()
        }
        verify("New veriff session is created") {
            val veriffSessions = veriffSessionRepository.findByUserAddressOrderByCreatedAtDesc(testContext.user.address)
            assertThat(veriffSessions).hasSize(2)
            verifyNewVeriffSession(veriffSessions.first())
        }
    }

    @Test
    fun mustHandleStartedEvent() {
        suppose("User has veriff session") {
            testContext.user = createUser()
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

        verify("Service will handle started event") {
            val data = getResourceAsText("/veriff/response-event-started.json")
            val session = veriffService.handleEvent(data) ?: fail("Missing session")
            assertThat(session.id).isEqualTo(testContext.veriffSession.id)
            assertThat(session.state).isEqualTo(VeriffSessionState.STARTED)
        }
    }

    @Test
    fun mustHandleSubmittedEvent() {
        suppose("User has veriff session") {
            testContext.user = createUser()
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

        verify("Service will handle started event") {
            val data = getResourceAsText("/veriff/response-event-submitted.json")
            val session = veriffService.handleEvent(data) ?: fail("Missing session")
            assertThat(session.id).isEqualTo(testContext.veriffSession.id)
            assertThat(session.state).isEqualTo(VeriffSessionState.SUBMITTED)
        }
    }

    @Test
    fun mustDeleteDecisionOnStartedEvent() {
        suppose("User has veriff session") {
            testContext.user = createUser()
            val veriffSession = VeriffSession(
                "cbb238c6-51a0-482b-bd1a-42a2e0b0ff1c",
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
        suppose("Veriff posted resubmission decision") {
            testContext.veriffDecision = veriffPostedDecision("/veriff/response-resubmission-for-started-event.json")
        }

        verify("Service will handle started event") {
            val data = getResourceAsText("/veriff/response-event-started.json")
            val session = veriffService.handleEvent(data) ?: fail("Missing session")
            assertThat(session.id).isEqualTo(testContext.veriffSession.id)
            assertThat(session.state).isEqualTo(VeriffSessionState.STARTED)
        }
        verify("Resubmission decision is deleted") {
            val veriffDecision = veriffDecisionRepository.findById(testContext.veriffDecision.id)
            assertThat(veriffDecision).isNotPresent
        }
    }

    @Test
    fun mustThrowExceptionForMissingUser() {
        verify("Service will throw ResourceNotFoundException exception") {
            val exception = assertThrows<ResourceNotFoundException> {
                veriffService.getVeriffSession("random_address", baseUrl)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_JWT_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionForMissingVerificationObject() {
        verify("Service will throw Veriff exception") {
            val veriffResponse = getResourceAsText("/veriff/response-missing-verification.json")
            assertThrows<VeriffException> {
                veriffService.handleDecision(veriffResponse)
            }
        }
    }

    @Test
    fun mustReturnNullUserInfoForDeclinedVerification() {
        verify("Service will return null for declined veriff response") {
            val veriffResponse = getResourceAsText("/veriff/response-declined.json")
            val userInfo = veriffService.handleDecision(veriffResponse)
            assertThat(userInfo).isNull()
        }
    }

    @Test
    fun mustVerifyUserForValidVendorData() {
        suppose("There is unverified user") {
            testContext.user = createUser()
        }

        verify("Service will store valid user data") {
            val veriffResponse = getResourceAsText("/veriff/response-with-vendor-data.json")
            testContext.userInfo = veriffService.handleDecision(veriffResponse) ?: fail("Missing user info")
        }
        verify("User is verified") {
            val user = userRepository.findByAddress(testContext.user.address)
            assertThat(user?.address).isEqualTo(testContext.user.address)
        }
        verify("User info is connected") {
            val userInfo = userInfoRepository.findById(testContext.userInfo.uuid).get()
            assertThat(userInfo.connected).isTrue()
        }
    }

    private fun veriffPostedDecision(file: String): VeriffDecision {
        val response = getResourceAsText(file)
        val veriffResponse: VeriffResponse = veriffService.mapVeriffResponse(response)
        val decision = VeriffDecision(veriffResponse.verification ?: fail("Missing verification"))
        return veriffDecisionRepository.save(decision)
    }

    private fun verifyNewVeriffSession(veriffSession: VeriffSession) {
        assertThat(veriffSession.id).isEqualTo("47679394-b37d-4932-86e6-d751f45ae546")
        assertThat(veriffSession.url)
            .isEqualTo("https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.new-url")
        assertThat(veriffSession.vendorData).isEqualTo("5ad36a1b-1c9e-4bf9-a88f-3c7fe68bdcf5")
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

    private class TestContext {
        lateinit var userInfo: UserInfo
        lateinit var user: User
        lateinit var veriffSession: VeriffSession
        lateinit var veriffDecision: VeriffDecision
    }
}
