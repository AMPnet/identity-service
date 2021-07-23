package com.ampnet.identityservice.controller

import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.config.DatabaseCleanerService
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.ErrorResponse
import com.ampnet.identityservice.persistence.model.Document
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.persistence.model.UserInfo
import com.ampnet.identityservice.persistence.repository.MailTokenRepository
import com.ampnet.identityservice.persistence.repository.RefreshTokenRepository
import com.ampnet.identityservice.persistence.repository.UserInfoRepository
import com.ampnet.identityservice.persistence.repository.UserRepository
import com.ampnet.identityservice.persistence.repository.VeriffDecisionRepository
import com.ampnet.identityservice.persistence.repository.VeriffSessionRepository
import com.ampnet.identityservice.service.MailService
import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.VerificationService
import com.ampnet.identityservice.service.Web3jService
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.kethereum.crypto.test_data.ADDRESS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@ExtendWith(value = [SpringExtension::class, RestDocumentationExtension::class])
@SpringBootTest
abstract class ControllerTestBase : TestBase() {

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService

    @Autowired
    protected lateinit var verificationService: VerificationService

    @Autowired
    protected lateinit var applicationProperties: ApplicationProperties

    @Autowired
    protected lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    protected lateinit var userInfoRepository: UserInfoRepository

    @Autowired
    protected lateinit var restTemplate: RestTemplate

    @Autowired
    protected lateinit var veriffSessionRepository: VeriffSessionRepository

    @Autowired
    protected lateinit var veriffDecisionRepository: VeriffDecisionRepository

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var mailTokenRepository: MailTokenRepository

    @Autowired
    protected lateinit var zonedDateTimeProvider: ZonedDateTimeProvider

    @Autowired
    protected lateinit var uuidProvider: UuidProvider

    @Autowired
    protected lateinit var web3jService: Web3jService

    @MockBean
    protected lateinit var mailService: MailService

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun init(wac: WebApplicationContext, restDocumentation: RestDocumentationContextProvider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
            .alwaysDo<DefaultMockMvcBuilder>(
                MockMvcRestDocumentation.document(
                    "{ClassName}/{methodName}",
                    Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                    Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
                )
            )
            .build()
    }

    protected fun getResponseErrorCode(errorCode: ErrorCode): String {
        return errorCode.categoryCode + errorCode.specificCode
    }

    protected fun verifyResponseErrorCode(result: MvcResult, errorCode: ErrorCode) {
        val response: ErrorResponse = objectMapper.readValue(result.response.contentAsString)
        val expectedErrorCode = getResponseErrorCode(errorCode)
        assert(response.errCode == expectedErrorCode)
    }

    protected fun createUser(
        address: String = ADDRESS.toString(),
        verified: Boolean = false,
        email: String? = "email@mail.com"
    ): User {
        var userInfo: UUID? = null
        if (verified) {
            val testUserInfo = UserInfo(
                uuidProvider.getUuid(), uuidProvider.getUuid().toString(), "first", "last",
                "id-num", "01-01-1001", Document(null, null, null, null, null),
                null, null, zonedDateTimeProvider.getZonedDateTime(), true, false
            )
            userInfo = userInfoRepository.save(testUserInfo).uuid
        }
        val user = User(address, email, userInfo, zonedDateTimeProvider.getZonedDateTime(), null)
        return userRepository.save(user)
    }
}
