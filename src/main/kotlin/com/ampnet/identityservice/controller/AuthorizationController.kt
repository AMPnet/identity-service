package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.request.AuthorizationRequest
import com.ampnet.identityservice.controller.pojo.request.AuthorizationRequestByMessage
import com.ampnet.identityservice.controller.pojo.request.PayloadRequest
import com.ampnet.identityservice.controller.pojo.request.RefreshTokenRequest
import com.ampnet.identityservice.controller.pojo.response.AccessRefreshTokenResponse
import com.ampnet.identityservice.controller.pojo.response.PayloadResponse
import com.ampnet.identityservice.service.TokenService
import com.ampnet.identityservice.service.UserService
import com.ampnet.identityservice.service.VerificationService
import com.ampnet.identityservice.util.ChainId
import com.ampnet.identityservice.util.WalletAddress
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthorizationController(
    private val verificationService: VerificationService,
    private val tokenService: TokenService,
    private val userService: UserService
) {

    companion object : KLogging()

    @PostMapping("/authorize")
    fun getPayload(@RequestBody request: PayloadRequest): ResponseEntity<PayloadResponse> {
        logger.debug { "Received request to get payload for address: ${request.address}" }
        val payload = verificationService.generatePayload(WalletAddress(request.address))
        return ResponseEntity.ok(PayloadResponse(payload))
    }

    @PostMapping("/authorize/by-message")
    fun getPayloadByMessage(): ResponseEntity<PayloadResponse> {
        logger.debug { "Received request to get payload by message" }
        val payload = verificationService.generatePayloadByMessage()
        return ResponseEntity.ok(PayloadResponse(payload))
    }

    @PostMapping("/authorize/jwt")
    fun authorizeJwt(@RequestBody request: AuthorizationRequest): ResponseEntity<AccessRefreshTokenResponse> {
        logger.debug { "Received request for token with address: ${request.address}" }
        verificationService.verifyPayload(
            address = WalletAddress(request.address),
            signedPayload = request.signedPayload,
            chainId = request.chainId?.let { ChainId(it) }
        )
        val accessAndRefreshToken = tokenService.generateAccessAndRefreshForUser(WalletAddress(request.address))
        logger.debug { "User address: ${request.address} successfully authorized." }
        userService.createUser(WalletAddress(request.address))
        return ResponseEntity.ok(AccessRefreshTokenResponse(accessAndRefreshToken))
    }

    @PostMapping("/authorize/jwt/by-message")
    fun jwtByMessage(@RequestBody request: AuthorizationRequestByMessage): ResponseEntity<AccessRefreshTokenResponse> {
        logger.debug { "Received request for token by message to sign: ${request.messageToSign}" }
        verificationService.verifyPayloadByMessage(request)
        val accessAndRefreshToken = tokenService.generateAccessAndRefreshForUser(WalletAddress(request.address))
        logger.debug { "User address: ${request.address} successfully authorized." }
        userService.createUser(WalletAddress(request.address))
        return ResponseEntity.ok(AccessRefreshTokenResponse(accessAndRefreshToken))
    }

    @PostMapping("/authorize/refresh")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<AccessRefreshTokenResponse> {
        logger.debug { "Received request to refresh token" }
        val accessAndRefreshToken = tokenService.generateAccessAndRefreshFromRefreshToken(request.refreshToken)
        return ResponseEntity.ok(AccessRefreshTokenResponse(accessAndRefreshToken))
    }
}
