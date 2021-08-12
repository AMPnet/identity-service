package com.ampnet.identityservice.blockchain

import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InternalException
import com.ampnet.identityservice.exception.InvalidRequestException
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import java.io.IOException
import java.math.BigInteger

private val logger = KotlinLogging.logger {}

@Service
class BlockchainServiceImpl(private val applicationProperties: ApplicationProperties) : BlockchainService {

    private val blockchainPropertiesMap = mutableMapOf<Long, BlockchainProperties>()

    @Suppress("MagicNumber")
    private val walletApproveGasLimit = BigInteger.valueOf(200_000)

    @Suppress("ReturnCount")
    override fun whitelistAddress(address: String, issuerAddress: String, chainId: Long): String? {
        logger.info { "Whitelisting address: $address on chain: $chainId for issuer: $issuerAddress" }
        val blockchainProperties = getBlockchainProperties(chainId)
        val nonce = blockchainProperties.web3j
            .ethGetTransactionCount(blockchainProperties.credentials.address, DefaultBlockParameterName.LATEST)
            .sendSafely()?.transactionCount ?: return null
        val gasPrice = blockchainProperties.web3j.ethGasPrice().sendSafely()?.gasPrice ?: return null

        val function = Function("approveWallet", listOf(issuerAddress.toAddress(), address.toAddress()), emptyList())
        val rawTransaction = RawTransaction.createTransaction(
            nonce, gasPrice, walletApproveGasLimit,
            blockchainProperties.walletApproverAddress, FunctionEncoder.encode(function)
        )

        val manager = RawTransactionManager(blockchainProperties.web3j, blockchainProperties.credentials, chainId)
        val sentTransaction = blockchainProperties.web3j
            .ethSendRawTransaction(manager.sign(rawTransaction)).sendSafely()
        logger.info { "Successfully whitelisted address: $address on chain: $chainId for issuer: $issuerAddress" }
        return sentTransaction?.transactionHash
    }

    override fun isMined(hash: String, chainId: Long): Boolean {
        val web3j = getBlockchainProperties(chainId).web3j
        val transaction = web3j.ethGetTransactionReceipt(hash).sendSafely()
        return transaction?.transactionReceipt?.isPresent ?: false
    }

    override fun isWhitelisted(address: String, issuerAddress: String, chainId: Long): Boolean {
        val web3j = getBlockchainProperties(chainId).web3j
        val transactionManager = ReadonlyTransactionManager(web3j, address)
        val contract = IIssuer.load(issuerAddress, web3j, transactionManager, DefaultGasProvider())
        return contract.isWalletApproved(address).sendSafely() ?: false
    }

    internal fun getChainRpcUrl(chain: Chain): String =
        if (chain.infura == null || applicationProperties.infuraId.isBlank()) {
            chain.rpcUrl
        } else {
            "${chain.infura}${applicationProperties.infuraId}"
        }

    private fun getBlockchainProperties(chainId: Long): BlockchainProperties {
        blockchainPropertiesMap[chainId]?.let { return it }
        val chain = Chain.fromId(chainId)
            ?: throw (InvalidRequestException(ErrorCode.BLOCKCHAIN_ID, "Blockchain id: $chainId not supported"))
        val properties = generateBlockchainProperties(chain)
        blockchainPropertiesMap[chainId] = properties
        return properties
    }

    private fun generateBlockchainProperties(chain: Chain): BlockchainProperties {
        val chainProperties = when (chain) {
            Chain.MATIC_MAIN -> applicationProperties.chainMatic
            Chain.MATIC_TESTNET_MUMBAI -> applicationProperties.chainMumbai
            Chain.ETHEREUM_MAIN -> applicationProperties.chainEthereum
            Chain.HARDHAT_TESTNET -> applicationProperties.chainHardhatTestnet
        }
        if (chainProperties.privateKey.isBlank() || chainProperties.walletApproverServiceAddress.isBlank())
            throw InternalException(
                ErrorCode.BLOCKCHAIN_CONFIG_MISSING,
                "Wallet approver config for chain: ${chain.name} not defined in the application properties"
            )
        val rpcUrl = getChainRpcUrl(chain)
        return BlockchainProperties(
            Credentials.create(chainProperties.privateKey),
            Web3j.build(HttpService(rpcUrl)),
            chainProperties.walletApproverServiceAddress
        )
    }

    private data class BlockchainProperties(
        val credentials: Credentials,
        val web3j: Web3j,
        val walletApproverAddress: String
    )
}

@Suppress("ReturnCount")
fun <S, T : Response<*>?> Request<S, T>.sendSafely(): T? {
    try {
        val value = this.send()
        if (value?.hasError() == true) {
            logger.warn { "Errors: ${value.error.message}" }
            return null
        }
        return value
    } catch (ex: IOException) {
        logger.warn("Failed blockchain call", ex)
        return null
    }
}

@Suppress("TooGenericExceptionCaught")
fun <T> RemoteFunctionCall<T>.sendSafely(): T? {
    return try {
        this.send()
    } catch (ex: Exception) {
        logger.warn("Failed smart contract call", ex)
        null
    }
}

@Suppress("MagicNumber")
fun String.toAddress(): Address = Address(160, this)
