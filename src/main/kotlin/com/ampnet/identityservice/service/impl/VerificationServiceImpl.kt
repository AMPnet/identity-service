package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.service.VerificationService
import org.springframework.stereotype.Service
import org.web3j.crypto.Sign
import kotlin.random.Random

@Service
class VerificationServiceImpl: VerificationService {

    private val userPayload = mutableMapOf<String, String>()

    override fun generatePayload(address: String) {
        val nonce: Long = Random.nextLong()
        userPayload[address] = nonce.toString()
    }

    override fun verifyPayload(address: String, signedPayload: String): Boolean {
        // signed payload is hashed
        val payload = userPayload[address] ?: throw Exception() // this is the nonce
        // Check if address, signed payload and nonce correspond
        val publicKey = Sign.signedMessageHashToKey(payload.toByteArray(), Sign.SignatureData())

        return false
    }
}