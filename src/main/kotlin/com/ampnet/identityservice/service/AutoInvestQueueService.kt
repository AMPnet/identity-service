package com.ampnet.identityservice.service

import com.ampnet.identityservice.controller.pojo.request.AutoInvestRequest
import com.ampnet.identityservice.controller.pojo.response.AutoInvestResponse
import com.ampnet.identityservice.util.ChainId
import com.ampnet.identityservice.util.ContractAddress
import com.ampnet.identityservice.util.WalletAddress

interface AutoInvestQueueService {
    fun createOrUpdateAutoInvestTask(
        address: WalletAddress,
        campaign: ContractAddress,
        chainId: ChainId,
        request: AutoInvestRequest
    ): AutoInvestResponse?
}
