package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.request.EmailRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController {

    @PostMapping("/user")
    fun updateEmail(@RequestBody emailRequest: EmailRequest) {
        val address = ControllerUtils.getAddressFromSecurityContext()
    }
}
