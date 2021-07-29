package com.ampnet.identityservice.service

import com.ampnet.identityservice.controller.pojo.request.KycTestRequest
import com.ampnet.identityservice.service.pojo.UserResponse
import java.util.UUID

interface UserService {
    fun getUserResponse(address: String): UserResponse
    fun connectUserInfo(userAddress: String, sessionId: String): UserResponse
    fun createUser(address: String): UserResponse
    fun updateEmail(email: String, address: String): UserResponse
    fun confirmMail(token: UUID): UserResponse?
    fun verifyUserWithTestData(request: KycTestRequest): UserResponse
    fun whitelistForIssuer(userAddress: String, issuerAddress: String)
}
