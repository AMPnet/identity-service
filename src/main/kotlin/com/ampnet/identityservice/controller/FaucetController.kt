package com.ampnet.identityservice.controller

import com.ampnet.identityservice.blockchain.properties.ChainPropertiesHandler
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.controller.pojo.request.ReCaptchaRequest
import com.ampnet.identityservice.exception.InternalException
import com.ampnet.identityservice.persistence.repository.BlockchainTaskRepository
import com.ampnet.identityservice.service.ReCaptchaService
import com.ampnet.identityservice.util.ChainId
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class FaucetController(
    applicationProperties: ApplicationProperties,
    private val blockchainTaskRepository: BlockchainTaskRepository,
    private val reCaptchaService: ReCaptchaService
) {

    private val chainHandler = ChainPropertiesHandler(applicationProperties)

    companion object : KLogging()

    @PostMapping("/faucet/{chainId}")
    fun requestFaucetFunds(
        @PathVariable chainId: Long,
        @RequestBody request: ReCaptchaRequest?
    ): ResponseEntity<Void> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        logger.debug { "Received faucet request for address: $address, chainId: $chainId" }
        reCaptchaService.validateResponseToken(request?.reCaptchaToken)

        val chainProperties = try {
            chainHandler.getBlockchainProperties(ChainId(chainId))
        } catch (e: InternalException) {
            logger.warn { "No properties exist for chainId: $chainId" }
            null
        }

        if (chainProperties?.faucet == null) {
            logger.warn { "Faucet not supported for chainId: $chainId" }
            return ResponseEntity.badRequest().build()
        }

        blockchainTaskRepository.addAddressToQueue(address.value, chainId)

        return ResponseEntity.ok(null)
    }
}
