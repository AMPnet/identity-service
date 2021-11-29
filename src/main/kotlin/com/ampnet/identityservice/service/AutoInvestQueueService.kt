package com.ampnet.identityservice.service

import com.ampnet.identityservice.controller.pojo.request.AutoInvestRequest
import com.ampnet.identityservice.controller.pojo.response.AutoInvestResponse

interface AutoInvestQueueService {
    fun createOrUpdateAutoInvestTask(address: String, chainId: Long, request: AutoInvestRequest): AutoInvestResponse?
}
