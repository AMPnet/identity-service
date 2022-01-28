package com.ampnet.identityservice.service

import com.ampnet.identityservice.persistence.model.UserInfo
import com.ampnet.identityservice.persistence.model.VeriffSession
import com.ampnet.identityservice.service.pojo.ServiceVerificationResponse
import com.ampnet.identityservice.util.WalletAddress

interface VeriffService {
    fun getVeriffSession(address: WalletAddress, baseUrl: String): ServiceVerificationResponse?
    fun handleDecision(data: String): UserInfo?
    fun handleEvent(data: String): VeriffSession?
    fun verifyClient(client: String)
    fun verifySignature(signature: String, data: String)
}
