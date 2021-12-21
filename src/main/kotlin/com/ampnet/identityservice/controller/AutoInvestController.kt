package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.request.AutoInvestRequest
import com.ampnet.identityservice.controller.pojo.response.AutoInvestResponse
import com.ampnet.identityservice.persistence.repository.AutoInvestTaskRepository
import com.ampnet.identityservice.service.AutoInvestQueueService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger

@RestController
class AutoInvestController(
    private val autoInvestQueueService: AutoInvestQueueService,
    private val autoInvestTaskRepository: AutoInvestTaskRepository
) {

    companion object : KLogging()

    @PostMapping("/auto_invest/{chainId}/{campaign}")
    fun addAutoInvestTask(
        @PathVariable chainId: Long,
        @PathVariable campaign: String,
        @RequestBody request: AutoInvestRequest
    ): ResponseEntity<AutoInvestResponse> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        logger.debug {
            "Received auto-invest request: $request for address: $address, campaign: $campaign and chainId: $chainId"
        }
        val response = autoInvestQueueService.createOrUpdateAutoInvestTask(address, campaign, chainId, request)
        return response?.let { ResponseEntity.ok(it) } ?: ResponseEntity.badRequest().build()
    }

    @GetMapping("/auto_invest/{chainId}/{campaign}")
    fun getAutoInvestTask(
        @PathVariable chainId: Long,
        @PathVariable campaign: String
    ): ResponseEntity<AutoInvestResponse> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        logger.debug { "Get auto-invest for address: $address, campaign: $campaign and chainId: $chainId" }
        val task = autoInvestTaskRepository.findByUserWalletAddressAndCampaignContractAddressAndChainId(
            userWalletAddress = address,
            campaignContractAddress = campaign,
            chainId = chainId
        )

        return ResponseEntity.ok(
            AutoInvestResponse(
                walletAddress = address,
                campaignAddress = campaign,
                amount = task?.amount ?: BigInteger.ZERO
            )
        )
    }
}
