package com.ampnet.identityservice.controller

import com.ampnet.identityservice.blockchain.properties.Chain
import com.ampnet.identityservice.controller.pojo.request.EmailRequest
import com.ampnet.identityservice.controller.pojo.request.WhitelistRequest
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException
import com.ampnet.identityservice.service.PinataService
import com.ampnet.identityservice.service.TokenService
import com.ampnet.identityservice.service.UserService
import com.ampnet.identityservice.service.impl.PinataResponse
import com.ampnet.identityservice.service.pojo.UserResponse
import com.ampnet.identityservice.util.ChainId
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userService: UserService,
    private val tokenService: TokenService,
    private val pinataService: PinataService
) {

    companion object : KLogging()

    @GetMapping("/user")
    fun getUser(): ResponseEntity<UserResponse> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        logger.debug { "Received request to get user data for address: $address" }
        return ResponseEntity.ok(userService.getUserResponse(address))
    }

    @PutMapping("/user")
    fun updateEmail(@RequestBody emailRequest: EmailRequest): ResponseEntity<UserResponse> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        logger.debug { "Received request to update mail for address: $address" }
        return ResponseEntity.ok(userService.updateEmail(emailRequest.email, address))
    }

    @PostMapping("/user/whitelist")
    fun whitelistForIssuer(@RequestBody request: WhitelistRequest): ResponseEntity<Unit> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        logger.debug { "Received request to whitelist address: $address for request: $request" }
        if (Chain.fromId(ChainId(request.chainId)) == null) {
            throw InvalidRequestException(ErrorCode.BLOCKCHAIN_ID, "Chain ID: ${request.chainId} not supported")
        }
        userService.whitelistAddress(address, request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/user/logout")
    fun logout(): ResponseEntity<Unit> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        logger.debug { "Received request to logout user: $address" }
        tokenService.deleteRefreshToken(address)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/user/pinata")
    fun getPinataJwt(): ResponseEntity<PinataResponse> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        logger.debug { "Received request to generate Pinata JWT for address: $address" }
        return ResponseEntity.ok(pinataService.getUserJwt(address))
    }
}
