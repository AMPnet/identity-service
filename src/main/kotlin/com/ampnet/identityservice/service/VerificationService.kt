package com.ampnet.identityservice.service

interface VerificationService {
    fun generatePayload(address: String): String
    fun verifyPayload(address: String, signedPayload: String): Boolean
}
