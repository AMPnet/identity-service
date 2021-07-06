package com.ampnet.identityservice.service

import com.ampnet.identityservice.controller.pojo.request.KycTestRequest
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.service.pojo.UserWithInfo

interface UserService {
    fun getUser(address: String): User
    fun connectUserInfo(userAddress: String, sessionId: String): User
    fun updateEmail(email: String, address: String): User
    fun createUser(address: String): User
    fun verifyUserWithUserInfo(request: KycTestRequest): UserWithInfo
}
