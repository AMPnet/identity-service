package com.ampnet.identityservice.service

import com.ampnet.identityservice.controller.pojo.request.KycTestRequest
import com.ampnet.identityservice.controller.pojo.request.WhitelistRequest
import com.ampnet.identityservice.service.pojo.UserResponse
import com.ampnet.identityservice.util.WalletAddress

interface UserService {
    fun getUserResponse(address: WalletAddress): UserResponse
    fun connectUserInfo(userAddress: WalletAddress, sessionId: String): UserResponse
    fun createUser(address: WalletAddress): UserResponse
    fun updateEmail(email: String, address: WalletAddress): UserResponse
    fun verifyUserWithTestData(request: KycTestRequest): UserResponse
    fun whitelistAddress(userAddress: WalletAddress, request: WhitelistRequest)
}
