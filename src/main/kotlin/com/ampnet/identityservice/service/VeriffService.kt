package com.ampnet.identityservice.service

import com.ampnet.identityservice.service.pojo.ServiceVerificationResponse
import java.util.UUID

interface VeriffService {
    fun getVeriffSession(address: String, baseUrl: String): ServiceVerificationResponse?
    fun handleDecision(data: String): UserInfo?
    fun handleEvent(data: String): VeriffSession?
    fun verifyClient(client: String)
    fun verifySignature(signature: String, data: String)
}
