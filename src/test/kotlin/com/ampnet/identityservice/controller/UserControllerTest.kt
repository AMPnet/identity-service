package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.request.EmailRequest
import com.ampnet.identityservice.controller.pojo.response.UserResponse
import com.ampnet.identityservice.persistence.model.MailToken
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.security.WithMockCrowdFundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class UserControllerTest : ControllerTestBase() {

    private lateinit var testContext: TestContext

    private val userPath = "/user"

    @BeforeEach
    fun init() {
        testContext = TestContext()
        databaseCleanerService.deleteAllUsers()
    }

    @Test
    @WithMockCrowdFundUser
    fun mustBeAbleToUpdateEmail() {
        suppose("User is existing") {
            testContext.user = createUser()
        }

        verify("User must be able to update email") {
            val request = objectMapper.writeValueAsString(EmailRequest(testContext.email))
            val result = mockMvc.perform(
                post(userPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
                .andExpect(status().isOk)
                .andReturn()
            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.address).isEqualTo(testContext.user.address)
            assertThat(userResponse.email).isNull()
        }
        verify("Email is set to null") {
            val user = userRepository.findByAddress(testContext.user.address)
            assertThat(user?.email).isNull()
        }
        verify("Email verification is sent") {
            Mockito.verify(mailService, Mockito.times(1))
                .sendEmailConfirmation(testContext.email)
        }
    }

    @Test
    @WithMockCrowdFundUser
    fun mustBeAbleToGetUser() {
        suppose("User is existing") {
            testContext.user = createUser()
        }

        verify("Must be able to get user data") {
            val result = mockMvc.perform(
                get(userPath)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()
            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.address).isEqualTo(testContext.user.address)
            assertThat(userResponse.email).isEqualTo(testContext.user.email)
        }
    }

    @Test
    @WithMockCrowdFundUser
    fun mustThrowExceptionForNonExistingUserOnEmailUpdate() {
        verify("Must return bad request") {
            val request = objectMapper.writeValueAsString(EmailRequest(testContext.email))
            mockMvc.perform(
                post(userPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
                .andExpect(status().isBadRequest)
                .andReturn()
        }
    }

    @Test
    @WithMockCrowdFundUser
    fun mustThrowExceptionForNonExistingUser() {
        verify("Must return bad request") {
            mockMvc.perform(
                get(userPath)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andReturn()
        }
    }

    @Test
    @WithMockCrowdFundUser
    fun mustBeAbleToConfirmEmail() {
        suppose("The user is created without email") {
            testContext.user = createUser()
        }
        suppose("The user has unconfirmed email") {
            testContext.token = uuidProvider.getUuid()
            val mailToken = MailToken(
                0, testContext.user.address, testContext.email,
                testContext.token, zonedDateTimeProvider.getZonedDateTime()
            )
            mailTokenRepository.save(mailToken)
        }

        verify("The user can confirm email with mail token") {
            mockMvc.perform(put("$userPath/email/${testContext.token}"))
                .andExpect(status().isOk)
        }
        verify("The user is confirmed in database") {
            val user = userRepository.findByAddress(testContext.user.address) ?: fail("Missing user")
            assertThat(user.email).isEqualTo(testContext.email)
        }
        verify("Mail token is deleted") {
            assertThat(mailTokenRepository.findByToken(testContext.token)).isNull()
        }
    }

    @Test
    @WithMockCrowdFundUser
    fun mustNotBeAbleToConfirmEmailWithExpiredToken() {
        suppose("The user is created with unconfirmed email") {
            testContext.user = createUser()
        }
        suppose("The token has expired") {
            testContext.token = uuidProvider.getUuid()
            val mailToken = MailToken(
                0, testContext.user.address, testContext.email, testContext.token,
                zonedDateTimeProvider.getZonedDateTime().minusDays(2)
            )
            mailTokenRepository.save(mailToken)
        }

        verify("The user cannot confirm email with expired token") {
            mockMvc.perform(put("$userPath/email/${testContext.token}"))
                .andExpect(status().isBadRequest)
        }
    }

    private class TestContext {
        lateinit var user: User
        lateinit var token: UUID
        val email = "new_email@gmail.com"
    }
}
