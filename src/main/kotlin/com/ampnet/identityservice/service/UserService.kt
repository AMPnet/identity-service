package com.ampnet.identityservice.service

import com.ampnet.identityservice.persistence.model.User

interface UserService {
    fun getUser(address: String): User
    fun connectUserInfo(userAddress: String, sessionId: String): User
    fun updateEmail(email: String, address: String): User
    fun createUser(address: String): User
}
