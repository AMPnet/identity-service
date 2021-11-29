package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.request.AutoInvestRequest
import com.ampnet.identityservice.controller.pojo.response.AutoInvestResponse
import com.ampnet.identityservice.persistence.model.AutoInvestTask
import com.ampnet.identityservice.persistence.model.AutoInvestTaskStatus
import com.ampnet.identityservice.persistence.repository.AutoInvestTaskRepository
import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AutoInvestController(
    private val autoInvestTaskRepository: AutoInvestTaskRepository,
    private val uuidProvider: UuidProvider,
    private val zonedDateTimeProvider: ZonedDateTimeProvider
) {

    companion object : KLogging()

    @PostMapping("/auto_invest/{chainId}")
    fun addAutoInvestTask(
        @PathVariable chainId: Long,
        @RequestBody request: AutoInvestRequest
    ): ResponseEntity<AutoInvestResponse> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        logger.debug { "Received auto-invest request: $request for address: $address and chainId: $chainId" }
        val numModified = autoInvestTaskRepository.createOrUpdate(
            AutoInvestTask(
                chainId = chainId,
                userWalletAddress = address,
                campaignContractAddress = request.campaignAddress,
                amount = request.amount,
                status = AutoInvestTaskStatus.PENDING,
                uuidProvider = uuidProvider,
                timeProvider = zonedDateTimeProvider
            )
        )

        return if (numModified == 0) {
            logger.warn {
                "Auto-invest already in process for address: $address, campaign: $${request.campaignAddress}," +
                    " chainId: $chainId"
            }
            ResponseEntity.badRequest().build()
        } else {
            val task = autoInvestTaskRepository.findByUserWalletAddressAndCampaignContractAddressAndChainId(
                userWalletAddress = address,
                campaignContractAddress = request.campaignAddress,
                chainId = chainId
            )
            ResponseEntity.ok(
                AutoInvestResponse(
                    walletAddress = task.userWalletAddress,
                    campaignAddress = task.campaignContractAddress,
                    amount = task.amount
                )
            )
        }
    }
}
