package com.ampnet.identityservice.blockchain

import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.contract.IIssuer
import com.ampnet.identityservice.service.unwrap
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import java.io.IOException

private val logger = KotlinLogging.logger {}

@Service
class BlockchainServiceImpl(private val applicationProperties: ApplicationProperties) : BlockchainService {

    private val web3j by lazy { Web3j.build(HttpService(applicationProperties.provider.blockchainApi)) }

    @Suppress("ReturnCount")
    override fun whitelistAddress(address: String): String? {
        logger.info { "Whitelisting address: $address" }
        val credentials = Credentials.create(applicationProperties.smartContract.privateKey)
        val contract = IIssuer.load(
            applicationProperties.smartContract.issuerContractAddress, web3j, credentials, DefaultGasProvider()
        )
        val approveWalletCall = contract.approveWallet(address).encodeFunctionCall()
        val nonce = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
            .sendSafely()?.transactionCount ?: return null
        val gasPrice = web3j.ethGasPrice().sendSafely()?.gasPrice ?: return null
        val transaction = Transaction
            .createContractTransaction(credentials.address, nonce, gasPrice, approveWalletCall)

        val transactionHash = web3j.ethSendTransaction(transaction).sendSafely()?.transactionHash
        logger.info { "Sent transaction: $transactionHash" }
        return transactionHash
    }

    override fun isMined(hash: String): Boolean {
        val transaction = web3j.ethGetTransactionReceipt(hash).sendSafely()
        return transaction?.transactionReceipt?.unwrap()?.isStatusOK ?: false
    }

    override fun isWhitelisted(address: String): Boolean {
        val transactionManager = ReadonlyTransactionManager(web3j, applicationProperties.smartContract.walletAddress)
        val contract = IIssuer.load(
            applicationProperties.smartContract.issuerContractAddress, web3j, transactionManager, DefaultGasProvider()
        )
        return contract.isWalletApproved(address).sendSafely() ?: false
    }
}

fun <S, T : Response<*>?> Request<S, T>.sendSafely(): T? {
    return try {
        this.send()
    } catch (ex: IOException) {
        logger.warn("Failed blockchain call", ex)
        null
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
