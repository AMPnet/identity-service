package com.ampnet.identityservice.service

interface Web3jService {
    fun approveWallet(address: String): String
    fun isWalletApproved(address: String): Boolean
}
