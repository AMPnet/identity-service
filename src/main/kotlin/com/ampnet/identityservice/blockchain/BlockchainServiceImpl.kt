package com.ampnet.identityservice.blockchain

import com.ampnet.identityservice.blockchain.properties.ChainPropertiesHandler
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.exception.InternalException
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.crypto.RawTransaction
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import java.io.IOException
import java.math.BigInteger

private val logger = KotlinLogging.logger {}

@Service
class BlockchainServiceImpl(
    applicationProperties: ApplicationProperties,
    private val restTemplate: RestTemplate
) : BlockchainService {

    private val chainHandler = ChainPropertiesHandler(applicationProperties)

    @Suppress("MagicNumber")
    private val walletApproveGasLimit = BigInteger.valueOf(200_000)

    @Suppress("ReturnCount")
    @Throws(InternalException::class)
    override fun whitelistAddress(address: String, issuerAddress: String, chainId: Long): String? {
        logger.info { "Whitelisting address: $address on chain: $chainId for issuer: $issuerAddress" }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainId)
        val nonce = blockchainProperties.web3j
            .ethGetTransactionCount(blockchainProperties.credentials.address, DefaultBlockParameterName.LATEST)
            .sendSafely()?.transactionCount ?: return null
        val gasPrice = getGasPrice(chainId)
        logger.debug { "Gas price: $gasPrice" }

        val function = Function("approveWallet", listOf(issuerAddress.toAddress(), address.toAddress()), emptyList())
        val rawTransaction = RawTransaction.createTransaction(
            nonce, gasPrice, walletApproveGasLimit,
            blockchainProperties.walletApproverAddress, FunctionEncoder.encode(function)
        )

        val manager = RawTransactionManager(blockchainProperties.web3j, blockchainProperties.credentials, chainId)
        val sentTransaction = blockchainProperties.web3j
            .ethSendRawTransaction(manager.sign(rawTransaction)).sendSafely()
        logger.info {
            "Successfully send request to whitelist address: $address on chain: $chainId for issuer: $issuerAddress"
        }
        return sentTransaction?.transactionHash
    }

    @Throws(InternalException::class)
    override fun isMined(hash: String, chainId: Long): Boolean {
        val web3j = chainHandler.getBlockchainProperties(chainId).web3j
        val transaction = web3j.ethGetTransactionReceipt(hash).sendSafely()
        return transaction?.transactionReceipt?.isPresent ?: false
    }

    @Throws(InternalException::class)
    override fun isWhitelisted(address: String, issuerAddress: String, chainId: Long): Boolean {
        val web3j = chainHandler.getBlockchainProperties(chainId).web3j
        val transactionManager = ReadonlyTransactionManager(web3j, address)
        val contract = IIssuer.load(issuerAddress, web3j, transactionManager, DefaultGasProvider())
        return contract.isWalletApproved(address).sendSafely() ?: false
    }

    internal fun getGasPrice(chainId: Long): BigInteger? {
        chainHandler.getGasPriceFeed(chainId)?.let { url ->
            try {
                val response = restTemplate
                    .getForObject<GasPriceFeedResponse>(url, GasPriceFeedResponse::class)
                response.fast?.let { price ->
                    val gWei = Convert.toWei(price.toString(), Convert.Unit.GWEI).toBigInteger()
                    logger.debug { "Fetched gas price in GWei: $gWei" }
                    return gWei
                }
            } catch (ex: RestClientException) {
                logger.warn { "Failed to get price for feed: $url" }
            }
        }
        return chainHandler.getBlockchainProperties(chainId)
            .web3j.ethGasPrice().sendSafely()?.gasPrice
    }

    private data class GasPriceFeedResponse(
        val safeLow: Long?,
        val standard: Long?,
        val fast: Long?,
        val fastest: Long?,
        val blockTime: Long?,
        val blockNumber: Long?
    )
}

@Suppress("ReturnCount")
fun <S, T : Response<*>?> Request<S, T>.sendSafely(): T? {
    try {
        val value = this.send()
        if (value?.hasError() == true) {
            logger.warn { "Web3j call errors: ${value.error.message}" }
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
