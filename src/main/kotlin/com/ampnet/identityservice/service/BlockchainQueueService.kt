package com.ampnet.identityservice.service

import com.ampnet.identityservice.controller.pojo.request.WhitelistRequest

interface BlockchainQueueService {
    fun createWhitelistAddressTask(address: String, request: WhitelistRequest)
}
