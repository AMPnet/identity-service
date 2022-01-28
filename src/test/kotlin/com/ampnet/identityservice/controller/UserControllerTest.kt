package com.ampnet.identityservice.controller

import com.ampnet.identityservice.blockchain.properties.Chain
import com.ampnet.identityservice.controller.pojo.request.EmailRequest
import com.ampnet.identityservice.controller.pojo.request.WhitelistRequest
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.persistence.model.RefreshToken
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.security.WithMockCrowdfundUser
import com.ampnet.identityservice.service.impl.PinataResponse
import com.ampnet.identityservice.service.pojo.UserResponse
import com.ampnet.identityservice.util.WalletAddress
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kethereum.crypto.test_data.ADDRESS
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.kotlin.verify as verifyMock

class UserControllerTest : ControllerTestBase() {

    private lateinit var testContext: TestContext

    private val userPath = "/user"
    private val whitelistPath = "$userPath/whitelist"
    private val logoutPath = "$userPath/logout"

    @BeforeEach
    fun init() {
        testContext = TestContext()
        databaseCleanerService.deleteAllUsers()
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToUpdateEmail() {
        suppose("User is existing") {
            testContext.user = createUser()
        }

        verify("User must be able to update email") {
            val request = objectMapper.writeValueAsString(EmailRequest(testContext.email))
            val result = mockMvc.perform(
                put(userPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
                .andExpect(status().isOk)
                .andReturn()
            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.address).isEqualTo(testContext.user.address)
            assertThat(userResponse.email).isEqualTo(testContext.email)
            assertThat(userResponse.emailVerified).isEqualTo(true)
            assertThat(userResponse.kycCompleted).isEqualTo(false)
        }
        verify("Email is set") {
            val user = userRepository.findByAddress(testContext.user.address)
            assertThat(user?.email).isEqualTo(testContext.email)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToGetUnverifiedUser() {
        suppose("User is unverified") {
            testContext.user = createUser(verified = false)
        }

        verify("Must be able to get user data for unverified user") {
            val result = mockMvc.perform(
                get(userPath)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()
            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.address).isEqualTo(testContext.user.address)
            assertThat(userResponse.email).isEqualTo(testContext.user.email)
            assertThat(userResponse.emailVerified).isEqualTo(true)
            assertThat(userResponse.kycCompleted).isEqualTo(false)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToGetVerifiedUser() {
        suppose("User is verified") {
            databaseCleanerService.deleteAllUserInfos()
            testContext.user = createUser(verified = true)
        }

        verify("Must be able to get user data for verified user") {
            val result = mockMvc.perform(
                get(userPath)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()
            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.address).isEqualTo(testContext.user.address)
            assertThat(userResponse.email).isEqualTo(testContext.user.email)
            assertThat(userResponse.emailVerified).isEqualTo(true)
            assertThat(userResponse.kycCompleted).isEqualTo(true)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustThrowExceptionForNonExistingUserOnEmailUpdate() {
        verify("Must return bad request") {
            val request = objectMapper.writeValueAsString(EmailRequest(testContext.email))
            mockMvc.perform(
                put(userPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
                .andExpect(status().isBadRequest)
                .andReturn()
        }
    }

    @Test
    @WithMockCrowdfundUser
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
    @WithMockCrowdfundUser
    fun mustBeAbleToLogoutUser() {
        suppose("Refresh token exists") {
            testContext.user = createUser()
            testContext.refreshToken = createRefreshToken(testContext.user.address)
        }

        verify("User can logout") {
            mockMvc.perform(post(logoutPath))
                .andExpect(status().isOk)
        }
        verify("Refresh token is deleted") {
            val optionalRefreshToken = refreshTokenRepository.findById(testContext.refreshToken.id)
            assertThat(optionalRefreshToken).isNotPresent
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToCallLogoutRouteWhenRefreshTokenDoesNotExist() {
        suppose("User exists") { testContext.user = createUser() }

        verify("User can call logout") {
            mockMvc.perform(post(logoutPath))
                .andExpect(status().isOk)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToWhitelistAddressForIssuer() {
        suppose("User has completed KYC") {
            testContext.user = createUser(
                verified = true,
                expiryDate = zonedDateTimeProvider.getZonedDateTime().plusDays(2).toLocalDate()
            )
        }

        verify("User can whitelist address for issuer") {
            testContext.whitelistRequest = WhitelistRequest(
                testContext.issuerAddress,
                Chain.MATIC_TESTNET_MUMBAI.id.value
            )
            val request = objectMapper.writeValueAsString(testContext.whitelistRequest)
            mockMvc.perform(
                post(whitelistPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
                .andExpect(status().isOk)
        }
        verify("Whitelisting user address has been called") {
            verifyMock(queueService)
                .addAddressToQueue(WalletAddress(testContext.user.address), testContext.whitelistRequest)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustNotBeAbleToWhitelistAddressForIssuerWithoutKyc() {
        suppose("User exists without KYC") {
            testContext.user = createUser(verified = false)
        }

        verify("User cannot whitelist address without KYC") {
            testContext.whitelistRequest = WhitelistRequest(
                testContext.issuerAddress,
                Chain.MATIC_TESTNET_MUMBAI.id.value
            )
            val request = objectMapper.writeValueAsString(testContext.whitelistRequest)
            val result = mockMvc.perform(
                post(whitelistPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.REG_VERIFF)
        }
        verify("Whitelisting user address has not been called") {
            verifyMock(queueService, times(0))
                .addAddressToQueue(WalletAddress(testContext.user.address), testContext.whitelistRequest)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustNotBeAbleToWhitelistAddressForExpiredDocument() {
        suppose("User exists without KYC") {
            testContext.user = createUser(
                expiryDate = zonedDateTimeProvider.getZonedDateTime().minusDays(2).toLocalDate()
            )
        }

        verify("User cannot whitelist address without KYC") {
            testContext.whitelistRequest = WhitelistRequest(
                testContext.issuerAddress,
                Chain.MATIC_TESTNET_MUMBAI.id.value
            )
            val request = objectMapper.writeValueAsString(testContext.whitelistRequest)
            val result = mockMvc.perform(
                post(whitelistPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.REG_VERIFF)
        }
        verify("Whitelisting user address has not been called") {
            verifyMock(queueService, times(0))
                .addAddressToQueue(WalletAddress(testContext.user.address), testContext.whitelistRequest)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustNotBeAbleToWhitelistAddressForInvalidChainId() {
        suppose("User exists without KYC") {
            testContext.user = createUser(verified = false)
        }

        verify("User cannot whitelist address without KYC") {
            testContext.whitelistRequest = WhitelistRequest(testContext.issuerAddress, -1L)
            val request = objectMapper.writeValueAsString(testContext.whitelistRequest)
            val result = mockMvc.perform(
                post(whitelistPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.BLOCKCHAIN_ID)
        }
        verify("Whitelisting user address has not been called") {
            verifyMock(queueService, times(0))
                .addAddressToQueue(WalletAddress(testContext.user.address), testContext.whitelistRequest)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToGetPinataJwt() {
        suppose("User exists") {
            testContext.user = createUser()
        }
        suppose("Pinata will return JWT") {
            testContext.pinataResponse = PinataResponse("api-key", "api-secret", "JWT")
            given(pinataService.getUserJwt(WalletAddress(ADDRESS.toString()))).willReturn(testContext.pinataResponse)
        }

        verify("User can get Pinata JWT") {
            val result = mockMvc.perform(
                get("$userPath/pinata")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()
            val pinataResponse: PinataResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(pinataResponse).isEqualTo(testContext.pinataResponse)
        }
    }

    private class TestContext {
        lateinit var user: User
        val email = "new_email@gmail.com"
        lateinit var refreshToken: RefreshToken
        val issuerAddress = "0xb070a65b1dd7f49c90a59000bd8cca3259064d81"
        lateinit var whitelistRequest: WhitelistRequest
        lateinit var pinataResponse: PinataResponse
    }
}
