package com.ampnet.identityservice.service

import org.springframework.stereotype.Service

@Service
interface BlockchainService {
    fun whitelistAddress(address: String): String?
    fun isMined(hash: String): Boolean
}
