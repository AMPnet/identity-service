package com.ampnet.identityservice.service

import com.ampnet.identityservice.controller.pojo.request.KycTestRequest
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.service.pojo.UserWithInfo
import java.util.UUID

interface UserService {
    fun getUser(address: String): User
    fun connectUserInfo(userAddress: String, sessionId: String): User
    fun createUser(address: String): User
    fun updateEmail(email: String, address: String): User
    fun confirmMail(token: UUID): User?
    fun verifyUserWithTestData(request: KycTestRequest): UserWithInfo
}
