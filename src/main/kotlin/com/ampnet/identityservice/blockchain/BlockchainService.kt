package com.ampnet.identityservice.blockchain

interface BlockchainService {
    fun whitelistAddress(address: String, issuerAddress: String): String?
    fun isMined(hash: String): Boolean
    fun isWhitelisted(address: String, issuerAddress: String): Boolean
}
