package com.ampnet.identityservice.service

import com.ampnet.identityservice.controller.pojo.request.WhitelistRequest

interface WhitelistQueueService {
    fun addAddressToQueue(address: String, request: WhitelistRequest)
}
