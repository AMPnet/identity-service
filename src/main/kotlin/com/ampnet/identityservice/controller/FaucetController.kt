package com.ampnet.identityservice.controller

import com.ampnet.identityservice.blockchain.properties.ChainPropertiesHandler
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.exception.InternalException
import com.ampnet.identityservice.persistence.repository.FaucetTaskRepository
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class FaucetController(
    applicationProperties: ApplicationProperties,
    private val faucetTaskRepository: FaucetTaskRepository
) {

    private val chainHandler = ChainPropertiesHandler(applicationProperties)

    companion object : KLogging()

    @PostMapping("/faucet/{chainId}/{address}")
    fun requestFaucetFunds(
        @PathVariable chainId: Long,
        @PathVariable address: String
    ): ResponseEntity<Void> {
        logger.debug { "Received faucet request for address: $address, chainId: $chainId" }

        val chainProperties = try {
            chainHandler.getBlockchainProperties(chainId)
        } catch (e: InternalException) {
            logger.warn { "No properties exist for chainId: $chainId" }
            null
        }

        if (chainProperties?.faucet == null) {
            logger.warn { "Faucet not supported for chainId: $chainId" }
            return ResponseEntity.badRequest().build()
        }

        faucetTaskRepository.addAddressToQueue(address, chainId)

        return ResponseEntity.ok(null)
    }
}
