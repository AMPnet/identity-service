package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.request.EmailRequest
import com.ampnet.identityservice.controller.pojo.response.UserResponse
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.security.WithMockCrowdFundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

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
            assertThat(userResponse.email).isEqualTo(testContext.email)
        }
        verify("Email is updated in the database") {
            val user = userRepository.findByAddress(testContext.user.address)
            assertThat(user?.email).isEqualTo(testContext.email)
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

    private class TestContext {
        lateinit var user: User
        val email = "new_email@gmail.com"
    }
}
