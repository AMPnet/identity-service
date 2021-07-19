package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.request.EmailRequest
import com.ampnet.identityservice.service.UserService
import com.ampnet.identityservice.service.pojo.UserResponse
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class UserController(private val userService: UserService) {

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

    @GetMapping("/user/email")
    fun confirmUserEmail(@RequestParam token: UUID): ResponseEntity<UserResponse> =
        userService.confirmMail(token)?.let { user ->
            ResponseEntity.ok(user)
        } ?: ResponseEntity.badRequest().build()
}
