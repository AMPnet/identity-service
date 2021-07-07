package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.request.KycTestRequest
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.security.WithMockCrowdFundUser
import com.ampnet.identityservice.service.pojo.UserWithInfo
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kethereum.crypto.test_data.ADDRESS
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TestKycControllerTest : ControllerTestBase() {

    private lateinit var testContext: TestContext

    private val customKycPath = "/test/kyc"

    @BeforeEach
    fun init() {
        testContext = TestContext()
        databaseCleanerService.deleteAllUsers()
        databaseCleanerService.deleteAllUserInfos()
    }

    @Test
    @WithMockCrowdFundUser
    fun mustBeAbleToVerifyUserWithTestData() {
        suppose("There is unverified user") {
            testContext.user = createUser()
        }

        verify("Must be able to verify user with test data") {
            testContext.request = KycTestRequest(ADDRESS.toString(), "John", "Doe")
            val result = mockMvc.perform(
                post(customKycPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testContext.request))
            )
                .andExpect(status().isOk)
                .andReturn()
            val userWithInfo: UserWithInfo = objectMapper.readValue(result.response.contentAsString)
            assertThat(userWithInfo.address).isEqualTo(testContext.user.address)
            assertThat(userWithInfo.firstName).isEqualTo(testContext.request.firstName)
            assertThat(userWithInfo.lastName).isEqualTo(testContext.request.lastName)
        }

        verify("TestData is stored in the database") {
            val user = userRepository.findByAddress(ADDRESS.toString()) ?: fail("User not found")
            val userInfoUuid = user.userInfoUuid ?: fail("User info uuid not set")
            val userInfo = userInfoRepository.findById(userInfoUuid).get()
            assertThat(userInfo.firstName).isEqualTo(testContext.request.firstName)
            assertThat(userInfo.lastName).isEqualTo(testContext.request.lastName)
            assertThat(user.userInfoUuid).isEqualTo(userInfo.uuid)
        }
    }

    @Test
    @WithMockCrowdFundUser
    fun mustReturnForbiddenIfTestKycIsDisabled() {
        suppose("There is unverified user") {
            testContext.user = createUser()
        }
        suppose("Test Kyc application property is disabled") {
            applicationProperties.test.enabledTestKyc = false
        }

        verify("Must return forbidden") {
            testContext.request = KycTestRequest(ADDRESS.toString(), "John", "Doe")
            mockMvc.perform(
                post(customKycPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testContext.request))
            )
                .andExpect(status().isForbidden)
                .andReturn()
        }
    }

    private class TestContext {
        lateinit var user: User
        lateinit var request: KycTestRequest
    }
}
