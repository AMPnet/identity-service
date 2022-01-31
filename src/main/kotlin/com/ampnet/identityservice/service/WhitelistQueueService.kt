package com.ampnet.identityservice.service

import com.ampnet.identityservice.controller.pojo.request.WhitelistRequest
import com.ampnet.identityservice.util.WalletAddress

interface WhitelistQueueService {
    fun addAddressToQueue(address: WalletAddress, request: WhitelistRequest)
}
