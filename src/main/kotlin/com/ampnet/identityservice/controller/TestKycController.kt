package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.request.KycTestRequest
import com.ampnet.identityservice.service.UserService
import com.ampnet.identityservice.service.pojo.UserWithInfo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TestKycController(private val userService: UserService) {

    @PostMapping("/test/kyc")
    fun customKyc(@RequestBody request: KycTestRequest): ResponseEntity<UserWithInfo> {
        return ResponseEntity.ok(userService.verifyUserWithUserInfo(request))
    }
}
