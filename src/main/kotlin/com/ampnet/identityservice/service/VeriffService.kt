package com.ampnet.identityservice.service

import com.ampnet.identityservice.persistence.model.UserInfo
import com.ampnet.identityservice.persistence.model.VeriffSession
import com.ampnet.identityservice.service.pojo.ServiceVerificationResponse

interface VeriffService {
    fun getVeriffSession(address: String, baseUrl: String): ServiceVerificationResponse?
    fun handleDecision(data: String): UserInfo?
    fun handleEvent(data: String): VeriffSession?
    fun verifyClient(client: String)
    fun verifySignature(signature: String, data: String)
}
