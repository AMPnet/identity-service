package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.AuthorizationRequest
import com.ampnet.identityservice.controller.pojo.AutohorizationRequest
import com.ampnet.identityservice.controller.pojo.getPayloadRequest
import com.ampnet.identityservice.controller.pojo.response.AccessRefreshTokenResponse
import com.ampnet.identityservice.controller.pojo.response.PayloadResponse
import com.ampnet.identityservice.service.TokenService
import com.ampnet.identityservice.service.VerificationService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

class AuthorizationController(
    private val verificationService: VerificationService,
    private val tokenService: TokenService
    ) {

    companion object : KLogging()

    @PostMapping("/authorize")
    fun authorizeAddress(@RequestBody request: getPayloadRequest): ResponseEntity<PayloadResponse> {
        // Generates nonce and returns that nonce to the frontend, then frontend
        // calls my authorizeJwt route to get the token details
//        logger.debug { "Received request to authorize user address: ${request.address}" }
//        reCaptchaService.validateResponseToken(request.reCaptchaToken)
//        val createUserRequest = createUserRequest(request)
//        validateRequestOrThrow(createUserRequest)
//        val user = userService.createUser(createUserRequest)
//        return ResponseEntity.ok(UserResponse(user))
    }

    @PostMapping("/authorize/jwt")
    fun authorizeJwt(@RequestBody request: AuthorizationRequest): ResponseEntity<AccessRefreshTokenResponse> {
        logger.debug { "Received request for token with address: ${request.address}" }
        val payloadVerified = verificationService.verifyPayload(request.address, request.signedPayload)
        //Recieve signed payload first verify payload and then generate Tokens
        val accessAndRefreshToken = tokenService.generateAccessAndRefreshForUser(request.address)
        logger.debug { "User address: ${request.address} successfully authenticated." }
//        val createUserRequest = createUserRequest(request)
//        validateRequestOrThrow(createUserRequest)
//        val user = userService.createUser(createUserRequest)
//        return ResponseEntity.ok(UserResponse(user))
    }
}