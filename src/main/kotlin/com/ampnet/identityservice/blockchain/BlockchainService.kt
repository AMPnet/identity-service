package com.ampnet.identityservice.blockchain

interface BlockchainService {
    fun whitelistAddress(address: String, issuerAddress: String, chainId: Long): String?
    fun isMined(hash: String, chainId: Long): Boolean
    fun isWhitelisted(address: String, issuerAddress: String, chainId: Long): Boolean
    fun sendFaucetFunds(addresses: List<String>, chainId: Long): String?
}
