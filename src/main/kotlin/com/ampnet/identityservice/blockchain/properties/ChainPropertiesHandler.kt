package com.ampnet.identityservice.blockchain.properties

import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.config.ChainProperties
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InternalException
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

class ChainPropertiesHandler(private val applicationProperties: ApplicationProperties) {

    private val blockchainPropertiesMap = mutableMapOf<Long, ChainPropertiesWithServices>()

    private val ChainProperties.faucet
        get() = CredentialsAndContractAddress(
            credentials = Credentials.create(this.faucetCallerPrivateKey),
            contractAddress = this.faucetServiceAddress
        )

    fun getBlockchainProperties(chainId: Long): ChainPropertiesWithServices {
        blockchainPropertiesMap[chainId]?.let { return it }
        val chain = getChain(chainId)
        val properties = generateBlockchainProperties(chain)
        blockchainPropertiesMap[chainId] = properties
        return properties
    }

    fun getGasPriceFeed(chainId: Long): String? = getChain(chainId).priceFeed

    internal fun getChainRpcUrl(chain: Chain): String =
        if (chain.infura == null || applicationProperties.infuraId.isBlank()) {
            chain.rpcUrl
        } else {
            "${chain.infura}${applicationProperties.infuraId}"
        }

    private fun generateBlockchainProperties(chain: Chain): ChainPropertiesWithServices {
        val chainProperties = getChainProperties(chain)
        val rpcUrl = getChainRpcUrl(chain)
        return ChainPropertiesWithServices(
            walletApprover = CredentialsAndContractAddress(
                credentials = Credentials.create(chainProperties.walletApproverPrivateKey),
                contractAddress = chainProperties.walletApproverServiceAddress
            ),
            faucet = if (chainProperties.faucetServiceEnabled) chainProperties.faucet else null,
            autoInvest = CredentialsAndContractAddress(
                credentials = Credentials.create(chainProperties.autoinvestPrivateKey),
                contractAddress = TODO()
            ),
            web3j = Web3j.build(HttpService(rpcUrl))
        )
    }

    private fun getChain(chainId: Long) = Chain.fromId(chainId)
        ?: throw InternalException(ErrorCode.BLOCKCHAIN_ID, "Blockchain id: $chainId not supported")

    private fun getChainProperties(chain: Chain): ChainProperties {
        val chainProperties = when (chain) {
            Chain.MATIC_MAIN -> applicationProperties.chainMatic
            Chain.MATIC_TESTNET_MUMBAI -> applicationProperties.chainMumbai
            Chain.ETHEREUM_MAIN -> applicationProperties.chainEthereum
            Chain.GOERLI_TESTNET -> applicationProperties.chainGoerli
            Chain.HARDHAT_TESTNET -> applicationProperties.chainHardhatTestnet
        }

        if (chainProperties.walletApproverPrivateKey.isBlank() ||
            chainProperties.walletApproverServiceAddress.isBlank()
        ) {
            throw InternalException(
                ErrorCode.BLOCKCHAIN_CONFIG_MISSING,
                "Wallet approver config for chain: ${chain.name} not defined in the application properties"
            )
        }

        if (chainProperties.faucetServiceEnabled &&
            (chainProperties.faucetCallerPrivateKey.isBlank() || chainProperties.faucetServiceAddress.isBlank())
        ) {
            throw InternalException(
                ErrorCode.BLOCKCHAIN_CONFIG_MISSING,
                "Faucet config for chain: ${chain.name} not defined in the application properties"
            )
        }

        if (chainProperties.autoinvestPrivateKey.isBlank()) {
            throw InternalException(
                ErrorCode.BLOCKCHAIN_CONFIG_MISSING,
                "Auto-invest wallet private key for chain: ${chain.name} not defined in the application properties"
            )
        }

        return chainProperties
    }
}
