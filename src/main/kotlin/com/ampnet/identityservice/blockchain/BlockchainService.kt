package com.ampnet.identityservice.blockchain

interface BlockchainService {
    fun whitelistAddress(address: String): String?
    fun isMined(hash: String): Boolean
    fun isWhitelisted(address: String): Boolean
}
