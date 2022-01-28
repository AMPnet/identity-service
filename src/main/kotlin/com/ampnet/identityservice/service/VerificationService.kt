package com.ampnet.identityservice.service

import com.ampnet.identityservice.util.WalletAddress

interface VerificationService {
    fun generatePayload(address: WalletAddress): String
    fun verifyPayload(address: WalletAddress, signedPayload: String)
}
