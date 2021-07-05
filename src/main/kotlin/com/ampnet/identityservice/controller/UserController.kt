package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.request.EmailRequest
import com.ampnet.identityservice.controller.pojo.response.UserResponse
import com.ampnet.identityservice.service.UserService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(private val userService: UserService) {

    companion object : KLogging()

    @PostMapping("/user")
    fun updateEmail(@RequestBody emailRequest: EmailRequest): ResponseEntity<UserResponse> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        logger.debug { "Received request to update mail for address: $address" }
        val user = userService.updateEmail(emailRequest.email, address)
        return ResponseEntity.ok(UserResponse(user))
    }

    @GetMapping("/user")
    fun getUser(): ResponseEntity<UserResponse> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        logger.debug { "Received request to get user data for address: $address" }
        val user = userService.getUser(address)
        return ResponseEntity.ok(UserResponse(user))
    }
}
