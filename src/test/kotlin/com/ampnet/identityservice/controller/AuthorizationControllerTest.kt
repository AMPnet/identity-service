package com.ampnet.identityservice.controller

import com.ampnet.core.jwt.JwtTokenUtils
import com.ampnet.identityservice.controller.pojo.request.AuthorizationRequest
import com.ampnet.identityservice.controller.pojo.request.PayloadRequest
import com.ampnet.identityservice.controller.pojo.request.RefreshTokenRequest
import com.ampnet.identityservice.controller.pojo.response.AccessRefreshTokenResponse
import com.ampnet.identityservice.controller.pojo.response.PayloadResponse
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.persistence.model.RefreshToken
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.util.WalletAddress
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kethereum.crypto.test_data.ADDRESS
import org.kethereum.crypto.test_data.KEY_PAIR
import org.kethereum.crypto.toHex
import org.kethereum.eip191.signWithEIP191PersonalSign
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthorizationControllerTest : ControllerTestBase() {

    private lateinit var testContext: TestContext

    private val authorizePath = "/authorize"
    private val authorizeJwtPath = "$authorizePath/jwt"
    private val tokenRefreshPath = "$authorizePath/refresh"

    @BeforeEach
    fun init() {
        testContext = TestContext()
        databaseCleanerService.deleteAllRefreshTokens()
    }

    @Test
    fun mustBeAbleToGetPayload() {
        verify("Controller returns payload") {
            val request = PayloadRequest(ADDRESS.toString())
            val result = mockMvc.perform(
                post(authorizePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            val payloadResponse: PayloadResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(payloadResponse).isNotNull
        }
    }

    @Test
    fun mustBeAbleToAuthorizeJwtForNewUser() {
        suppose("There are no users in database") {
            databaseCleanerService.deleteAllUsers()
        }
        suppose("Client signs the payload") {
            val payload = verificationService.generatePayload(WalletAddress(ADDRESS.toString()))
            testContext.signedPayload = "0x" + KEY_PAIR.signWithEIP191PersonalSign(payload.toByteArray()).toHex()
        }

        verify("Client is authorized and gets jwt") {
            val request = AuthorizationRequest(ADDRESS.toString(), testContext.signedPayload, 0)
            val result = mockMvc.perform(
                post(authorizeJwtPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            val response: AccessRefreshTokenResponse = objectMapper.readValue(result.response.contentAsString)
            verifyAccessRefreshTokenResponse(response)
        }
        verify("User is created") {
            val user = userRepository.findByAddress(ADDRESS.toString())
            assertThat(user).isNotNull
        }
    }

    @Test
    fun mustBeAbleToAuthorizeJwtForExistingUser() {
        suppose("There is a user") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = createUser()
        }
        suppose("Client signs the payload") {
            val payload = verificationService.generatePayload(WalletAddress(ADDRESS.toString()))
            testContext.signedPayload = "0x" + KEY_PAIR.signWithEIP191PersonalSign(payload.toByteArray()).toHex()
        }

        verify("Client is authorized and gets jwt") {
            val request = AuthorizationRequest(ADDRESS.toString(), testContext.signedPayload, 0)
            val result = mockMvc.perform(
                post(authorizeJwtPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            val response: AccessRefreshTokenResponse = objectMapper.readValue(result.response.contentAsString)
            verifyAccessRefreshTokenResponse(response)
        }
        verify("New user is not created in database") {
            assertThat(userRepository.count()).isEqualTo(1)
        }
    }

    @Test
    fun mustBeAbleToGetAccessTokenWithRefreshToken() {
        suppose("Refresh token exists") {
            testContext.refreshToken = createRefreshToken(
                ADDRESS.toString(), zonedDateTimeProvider.getZonedDateTime().minusHours(1)
            )
        }

        verify("User can get access token using refresh token") {
            val request = RefreshTokenRequest(testContext.refreshToken.token)
            val result = mockMvc.perform(
                post(tokenRefreshPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andReturn()

            val response: AccessRefreshTokenResponse = objectMapper.readValue(result.response.contentAsString)
            verifyTokenForUserAddress(response.accessToken)
            assertThat(response.expiresIn).isEqualTo(applicationProperties.jwt.accessTokenValidityInMilliseconds())
            assertThat(response.refreshToken).isEqualTo(testContext.refreshToken.token)
            assertThat(response.refreshTokenExpiresIn)
                .isLessThan(applicationProperties.jwt.refreshTokenValidityInMilliseconds())
        }
    }

    @Test
    fun mustNotBeAbleToGetAccessTokenWithExpiredRefreshToken() {
        suppose("Refresh token expired") {
            val createdAt = zonedDateTimeProvider.getZonedDateTime()
                .minusMinutes(applicationProperties.jwt.refreshTokenValidityInMinutes + 1000L)
            testContext.refreshToken = createRefreshToken(ADDRESS.toString(), createdAt)
        }

        verify("User will get bad request response") {
            val request = RefreshTokenRequest(testContext.refreshToken.token)
            val response = mockMvc.perform(
                post(tokenRefreshPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(response, ErrorCode.AUTH_INVALID_REFRESH_TOKEN)
        }
    }

    @Test
    fun mustNotBeAbleToGetAccessTokenWithNonExistingRefreshToken() {
        verify("User will get bad request response") {
            val request = RefreshTokenRequest("non-existing-refresh-token")
            val response = mockMvc.perform(
                post(tokenRefreshPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(response, ErrorCode.AUTH_INVALID_REFRESH_TOKEN)
        }
    }

    private fun verifyAccessRefreshTokenResponse(response: AccessRefreshTokenResponse) {
        verifyTokenForUserAddress(response.accessToken)
        assertThat(response.expiresIn).isEqualTo(applicationProperties.jwt.accessTokenValidityInMilliseconds())
        assertThat(response.refreshToken).isNotNull
        assertThat(response.refreshTokenExpiresIn)
            .isEqualTo(applicationProperties.jwt.refreshTokenValidityInMilliseconds())
    }

    private fun verifyTokenForUserAddress(token: String) {
        val address: String = JwtTokenUtils.decodeToken(token, applicationProperties.jwt.publicKey)
        assertThat(address).isEqualTo(ADDRESS.toString())
    }

    private class TestContext {
        lateinit var signedPayload: String
        lateinit var refreshToken: RefreshToken
        lateinit var user: User
    }
}
