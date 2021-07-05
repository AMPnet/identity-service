package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.request.AuthorizationRequest
import com.ampnet.identityservice.controller.pojo.request.PayloadRequest
import com.ampnet.identityservice.controller.pojo.request.RefreshTokenRequest
import com.ampnet.identityservice.controller.pojo.response.AccessRefreshTokenResponse
import com.ampnet.identityservice.controller.pojo.response.PayloadResponse
import com.ampnet.identityservice.service.TokenService
import com.ampnet.identityservice.service.VerificationService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthorizationController(
    private val verificationService: VerificationService,
    private val tokenService: TokenService
) {

    companion object : KLogging()

    @PostMapping("/authorize")
    fun getPayload(@RequestBody request: PayloadRequest): ResponseEntity<PayloadResponse> {
        logger.debug { "Received request to get payload for address: ${request.address}" }
        val payload = verificationService.generatePayload(request.address)
        return ResponseEntity.ok(PayloadResponse(payload))
    }

    @PostMapping("/authorize/jwt")
    fun authorizeJwt(@RequestBody request: AuthorizationRequest): ResponseEntity<AccessRefreshTokenResponse> {
        logger.debug { "Received request for token with address: ${request.address}" }
        val payloadValid = verificationService.verifyPayload(request.address, request.signedPayload)
        if (payloadValid.not()) return ResponseEntity.badRequest().build()
        val accessAndRefreshToken = tokenService.generateAccessAndRefreshForUser(request.address)
        logger.debug { "User address: ${request.address} successfully authorized." }
        return ResponseEntity.ok(AccessRefreshTokenResponse(accessAndRefreshToken))
    }

    @PostMapping("/token/refresh")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<AccessRefreshTokenResponse> {
        logger.debug { "Received request to refresh token" }
        val accessAndRefreshToken = tokenService.generateAccessAndRefreshFromRefreshToken(request.refreshToken)
        return ResponseEntity.ok(AccessRefreshTokenResponse(accessAndRefreshToken))
    }
}
