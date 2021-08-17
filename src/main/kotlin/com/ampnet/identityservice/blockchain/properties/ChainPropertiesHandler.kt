package com.ampnet.identityservice.blockchain.properties

import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InternalException
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

class ChainPropertiesHandler(private val applicationProperties: ApplicationProperties) {

    private val blockchainPropertiesMap = mutableMapOf<Long, ChainPropertiesWithServices>()

    fun getBlockchainProperties(chainId: Long): ChainPropertiesWithServices {
        blockchainPropertiesMap[chainId]?.let { return it }
        val chain = Chain.fromId(chainId)
            ?: throw InternalException(ErrorCode.BLOCKCHAIN_ID, "Blockchain id: $chainId not supported")
        val properties = generateBlockchainProperties(chain)
        blockchainPropertiesMap[chainId] = properties
        return properties
    }

    internal fun getChainRpcUrl(chain: Chain): String =
        if (chain.infura == null || applicationProperties.infuraId.isBlank()) {
            chain.rpcUrl
        } else {
            "${chain.infura}${applicationProperties.infuraId}"
        }

    private fun generateBlockchainProperties(chain: Chain): ChainPropertiesWithServices {
        val chainProperties = when (chain) {
            Chain.MATIC_MAIN -> applicationProperties.chainMatic
            Chain.MATIC_TESTNET_MUMBAI -> applicationProperties.chainMumbai
            Chain.ETHEREUM_MAIN -> applicationProperties.chainEthereum
            Chain.HARDHAT_TESTNET -> applicationProperties.chainHardhatTestnet
        }
        if (chainProperties.privateKey.isBlank() || chainProperties.walletApproverServiceAddress.isBlank()) {
            throw InternalException(
                ErrorCode.BLOCKCHAIN_CONFIG_MISSING,
                "Wallet approver config for chain: ${chain.name} not defined in the application properties"
            )
        }
        val rpcUrl = getChainRpcUrl(chain)
        return ChainPropertiesWithServices(
            Credentials.create(chainProperties.privateKey),
            Web3j.build(HttpService(rpcUrl)),
            chainProperties.walletApproverServiceAddress
        )
    }
}
