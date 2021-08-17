package com.ampnet.identityservice.blockchain

import com.ampnet.identityservice.blockchain.properties.Chain
import com.ampnet.identityservice.blockchain.properties.ChainPropertiesHandler
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InternalException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChainPropertiesHandlerTest {

    @Test
    fun mustThrowExceptionForInvalidChainId() {
        val chainPropertiesHandler = ChainPropertiesHandler(ApplicationProperties())
        val exception = assertThrows<InternalException> {
            chainPropertiesHandler.getBlockchainProperties(-1)
        }
        assertThat(exception.errorCode).isEqualTo(ErrorCode.BLOCKCHAIN_ID)
    }

    @Test
    fun mustReturnDefaultRpcIfInfuraIdIsMissing() {
        val applicationProperties = ApplicationProperties().apply { infuraId = "" }
        val chainPropertiesHandler = ChainPropertiesHandler(applicationProperties)
        val chain = Chain.MATIC_TESTNET_MUMBAI
        val rpc = chainPropertiesHandler.getChainRpcUrl(chain)
        assertThat(rpc).isEqualTo(chain.rpcUrl)
    }

    @Test
    fun mustReturnInfuraRpc() {
        val infuraId = "some-id"
        val applicationProperties = ApplicationProperties().apply { this.infuraId = infuraId }
        val chainPropertiesHandler = ChainPropertiesHandler(applicationProperties)
        val chain = Chain.MATIC_TESTNET_MUMBAI
        val rpc = chainPropertiesHandler.getChainRpcUrl(chain)
        assertThat(rpc).isEqualTo(chain.infura + infuraId)
    }

    @Test
    fun mustThrowExceptionForMissingChainConfig() {
        val exception = assertThrows<InternalException> {
            val applicationProperties = ApplicationProperties().apply { this.chainEthereum.privateKey = "" }
            val chainPropertiesHandler = ChainPropertiesHandler(applicationProperties)
            chainPropertiesHandler.getBlockchainProperties(Chain.ETHEREUM_MAIN.id)
        }
        assertThat(exception.errorCode).isEqualTo(ErrorCode.BLOCKCHAIN_CONFIG_MISSING)
    }
}
