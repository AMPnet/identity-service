package com.ampnet.identityservice.controller

import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.controller.pojo.request.KycTestRequest
import com.ampnet.identityservice.service.UserService
import com.ampnet.identityservice.service.pojo.UserResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TestKycController(
    private val userService: UserService,
    private val applicationProperties: ApplicationProperties
) {

    @PostMapping("/test/kyc")
    fun customKyc(@RequestBody request: KycTestRequest): ResponseEntity<UserResponse> {
        if (applicationProperties.test.enabledTestKyc.not()) return ResponseEntity(HttpStatus.FORBIDDEN)
        return ResponseEntity.ok(userService.verifyUserWithTestData(request))
    }
}
