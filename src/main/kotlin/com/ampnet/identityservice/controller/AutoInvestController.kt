package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.request.AutoInvestRequest
import com.ampnet.identityservice.controller.pojo.response.AutoInvestResponse
import com.ampnet.identityservice.service.AutoInvestQueueService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AutoInvestController(private val autoInvestQueueService: AutoInvestQueueService) {

    companion object : KLogging()

    @PostMapping("/auto_invest/{chainId}")
    fun addAutoInvestTask(
        @PathVariable chainId: Long,
        @RequestBody request: AutoInvestRequest
    ): ResponseEntity<AutoInvestResponse> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        logger.debug { "Received auto-invest request: $request for address: $address and chainId: $chainId" }
        val response = autoInvestQueueService.createOrUpdateAutoInvestTask(address, chainId, request)
        return response?.let { ResponseEntity.ok(it) } ?: ResponseEntity.badRequest().build()
    }
}
