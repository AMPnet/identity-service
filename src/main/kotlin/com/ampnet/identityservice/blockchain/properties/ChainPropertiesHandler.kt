package com.ampnet.identityservice.blockchain.properties

import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.config.ChainProperties
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InternalException
import com.ampnet.identityservice.util.ChainId
import com.ampnet.identityservice.util.ContractAddress
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

class ChainPropertiesHandler(private val applicationProperties: ApplicationProperties) {

    private val blockchainPropertiesMap = mutableMapOf<ChainId, ChainPropertiesWithServices>()

    private val ChainProperties.faucet
        get() = CredentialsAndContractAddress(
            credentials = Credentials.create(this.faucetCallerPrivateKey),
            contractAddress = ContractAddress(this.faucetServiceAddress)
        )

    private val ChainProperties.autoInvest
        get() = CredentialsAndContractAddress(
            credentials = Credentials.create(this.autoInvestPrivateKey),
            contractAddress = ContractAddress(this.autoInvestServiceAddress)
        )

    fun getBlockchainProperties(chainId: ChainId): ChainPropertiesWithServices {
        return blockchainPropertiesMap.computeIfAbsent(chainId) {
            generateBlockchainProperties(getChain(it))
        }
    }

    fun getGasPriceFeed(chainId: ChainId): String? = getChain(chainId).priceFeed

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
                contractAddress = ContractAddress(chainProperties.walletApproverServiceAddress)
            ),
            faucet = if (isFaucetAvailable(chainProperties)) chainProperties.faucet else null,
            autoInvest = if (isAutoInvestAvailable(chainProperties)) chainProperties.autoInvest else null,
            web3j = Web3j.build(HttpService(rpcUrl))
        )
    }

    private fun getChain(chainId: ChainId) = Chain.fromId(chainId)
        ?: throw InternalException(ErrorCode.BLOCKCHAIN_ID, "Blockchain id: $chainId not supported")

    @Suppress("ThrowsCount")
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

        if (!isFaucetAvailable(chainProperties)) {
            throw InternalException(
                ErrorCode.BLOCKCHAIN_CONFIG_MISSING,
                "Faucet config for chain: ${chain.name} not defined in the application properties"
            )
        }

        if (!isAutoInvestAvailable(chainProperties)) {
            throw InternalException(
                ErrorCode.BLOCKCHAIN_CONFIG_MISSING,
                "Auto-invest config for chain: ${chain.name} not defined in the application properties"
            )
        }

        return chainProperties
    }

    private fun isAutoInvestAvailable(chainProperties: ChainProperties): Boolean =
        applicationProperties.autoInvest.enabled &&
            chainProperties.autoInvestPrivateKey.isNotBlank() &&
            chainProperties.autoInvestServiceAddress.isNotBlank()

    private fun isFaucetAvailable(chainProperties: ChainProperties): Boolean =
        applicationProperties.faucet.enabled &&
            chainProperties.faucetCallerPrivateKey.isNotBlank() &&
            chainProperties.faucetServiceAddress.isNotBlank()
}
