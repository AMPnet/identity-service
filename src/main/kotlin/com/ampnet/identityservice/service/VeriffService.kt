package com.ampnet.identityservice.service

import java.util.UUID

interface VeriffService {
    fun getVeriffSession(userUuid: UUID, baseUrl: String): ServiceVerificationResponse?
    fun handleDecision(data: String): UserInfo?
    fun handleEvent(data: String): VeriffSession?
    fun verifyClient(client: String)
    fun verifySignature(signature: String, data: String)
}
