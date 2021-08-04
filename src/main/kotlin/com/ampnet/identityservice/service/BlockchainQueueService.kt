package com.ampnet.identityservice.service

interface BlockchainQueueService {
    fun createWhitelistAddressTask(address: String, issuerAddress: String)
}
