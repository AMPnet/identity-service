package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.request.EmailRequest
import com.ampnet.identityservice.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(private val userService: UserService) {

    @PostMapping("/user")
    fun updateEmail(@RequestBody emailRequest: EmailRequest): ResponseEntity<Unit> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        userService.updateEmail(emailRequest.email, address)
        return ResponseEntity.ok().build()
    }
}
