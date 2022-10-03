package com.ampnet.identityservice.service

import com.ampnet.identityservice.controller.pojo.request.AuthorizationRequestByMessage
import com.ampnet.identityservice.util.ChainId
import com.ampnet.identityservice.util.WalletAddress

interface VerificationService {
    fun generatePayload(address: WalletAddress): String
    fun generatePayloadByMessage(): String
    fun verifyPayload(address: WalletAddress, signedPayload: String, chainId: ChainId?)
    fun verifyPayloadByMessage(request: AuthorizationRequestByMessage)
}
