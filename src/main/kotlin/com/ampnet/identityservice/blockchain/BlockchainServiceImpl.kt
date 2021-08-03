package com.ampnet.identityservice.blockchain

import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException
import com.ampnet.identityservice.service.unwrap
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

    private val web3js = mutableMapOf<Long, Web3j>()
    private val credentials: Credentials by lazy { Credentials.create(applicationProperties.smartContract.privateKey) }

    @Suppress("ReturnCount", "MagicNumber")
    override fun whitelistAddress(address: String, issuerAddress: String, chainId: Long): String? {
        logger.info { "Whitelisting address: $address on chain: $chainId for issuer: $issuerAddress" }
        val web3j = getWeb3j(chainId)
        val nonce = web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.LATEST)
            .sendSafely()?.transactionCount ?: return null
        val gasPrice = web3j.ethGasPrice().sendSafely()?.gasPrice ?: return null
        val function = Function(
            "approveWallet", listOf(issuerAddress.toAddress(), address.toAddress()), emptyList()
        )
        val encoded = FunctionEncoder.encode(function)
        val manager = RawTransactionManager(web3j, credentials, chainId)
        val rawTransaction = RawTransaction.createTransaction(
            nonce, gasPrice, BigInteger.valueOf(200_000),
            applicationProperties.smartContract.walletApproverServiceAddress, encoded
        )
        val sentTransaction = web3j.ethSendRawTransaction(manager.sign(rawTransaction)).sendSafely()
        return sentTransaction?.transactionHash
    }

    override fun isMined(hash: String, chainId: Long): Boolean {
        val web3j = getWeb3j(chainId)
        val transaction = web3j.ethGetTransactionReceipt(hash).sendSafely()
        return transaction?.transactionReceipt?.unwrap()?.isStatusOK ?: false
    }

    override fun isWhitelisted(address: String, issuerAddress: String, chainId: Long): Boolean {
        val web3j = getWeb3j(chainId)
        val transactionManager = ReadonlyTransactionManager(web3j, address)
        val contract = IIssuer.load(issuerAddress, web3j, transactionManager, DefaultGasProvider())
        return contract.isWalletApproved(address).sendSafely() ?: false
    }

    private fun getWeb3j(chainId: Long): Web3j {
        web3js[chainId]?.let { return it }
        val chain = Chain.fromId(chainId)
            ?: throw (InvalidRequestException(ErrorCode.BLOCKCHAIN_ID, "Blockchain id: $chainId not supported"))
        val web3j = Web3j.build(HttpService(chain.infura + applicationProperties.provider.infuraId))
        web3js[chainId] = web3j
        return web3j
    }
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
