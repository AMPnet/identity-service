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
    fun mustThrowExceptionForMissingWalletApproverChainConfig() {
        val exception = assertThrows<InternalException> {
            val applicationProperties = ApplicationProperties().apply {
                this.chainEthereum.walletApproverPrivateKey = ""
            }
            val chainPropertiesHandler = ChainPropertiesHandler(applicationProperties)
            chainPropertiesHandler.getBlockchainProperties(Chain.ETHEREUM_MAIN.id)
        }
        assertThat(exception.errorCode).isEqualTo(ErrorCode.BLOCKCHAIN_CONFIG_MISSING)
    }

    @Test
    fun mustThrowExceptionForMissingFaucetChainConfig() {
        val exception = assertThrows<InternalException> {
            val applicationProperties = ApplicationProperties().apply {
                this.chainEthereum.walletApproverPrivateKey = "test-key"
                this.chainEthereum.walletApproverServiceAddress = "test-address"
                this.chainEthereum.faucetServiceEnabled = true
                this.chainEthereum.faucetCallerPrivateKey = ""
            }
            val chainPropertiesHandler = ChainPropertiesHandler(applicationProperties)
            chainPropertiesHandler.getBlockchainProperties(Chain.ETHEREUM_MAIN.id)
        }
        assertThat(exception.errorCode).isEqualTo(ErrorCode.BLOCKCHAIN_CONFIG_MISSING)
    }

    @Test
    fun mustThrowExceptionForMissingAutoInvestChainConfig() {
        val exception = assertThrows<InternalException> {
            val applicationProperties = ApplicationProperties().apply {
                this.chainEthereum.walletApproverPrivateKey = "test-key"
                this.chainEthereum.walletApproverServiceAddress = "test-address"
                this.chainEthereum.faucetServiceEnabled = true
                this.chainEthereum.faucetCallerPrivateKey = "test-key"
                this.chainEthereum.faucetServiceAddress = "test-address"
                this.chainEthereum.autoInvestPrivateKey = ""
            }
            val chainPropertiesHandler = ChainPropertiesHandler(applicationProperties)
            chainPropertiesHandler.getBlockchainProperties(Chain.ETHEREUM_MAIN.id)
        }
        assertThat(exception.errorCode).isEqualTo(ErrorCode.BLOCKCHAIN_CONFIG_MISSING)
    }
}
