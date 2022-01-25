package com.ampnet.identityservice.blockchain

import com.ampnet.identityservice.blockchain.IInvestService.InvestmentRecord
import com.ampnet.identityservice.blockchain.IInvestService.InvestmentRecordStatus
import com.ampnet.identityservice.blockchain.properties.ChainPropertiesHandler
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InternalException
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.tx.gas.StaticGasProvider
import org.web3j.utils.Convert
import java.io.IOException
import java.math.BigInteger

private val logger = KotlinLogging.logger {}

@Service
class BlockchainServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val restTemplate: RestTemplate
) : BlockchainService {

    private val chainHandler = ChainPropertiesHandler(applicationProperties)

    @Suppress("ReturnCount")
    @Throws(InternalException::class)
    override fun whitelistAddresses(addresses: List<String>, issuerAddress: String, chainId: Long): String? {
        logger.info { "Whitelisting addresses: $addresses on chain: $chainId for issuer: $issuerAddress" }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainId)
        val gasPrice = getGasPrice(chainId)
        logger.debug { "Gas price: $gasPrice" }

        val transactionManager = RawTransactionManager(
            blockchainProperties.web3j,
            blockchainProperties.walletApprover.credentials,
            chainId
        )
        val gasProvider = StaticGasProvider(gasPrice, applicationProperties.walletApprove.gasLimit)
        val walletApproverContract = WalletApproverService.load(
            blockchainProperties.walletApprover.contractAddress,
            blockchainProperties.web3j,
            transactionManager,
            gasProvider
        )

        val sentTransaction = walletApproverContract.approveWallets(issuerAddress, addresses).sendSafely()

        logger.info {
            "Successfully send request to whitelist addresses: $addresses on chain: $chainId for issuer: $issuerAddress"
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
    override fun isWhitelisted(address: String, issuerAddress: String?, chainId: Long): Boolean {
        if (issuerAddress == null)
            return false
        val web3j = chainHandler.getBlockchainProperties(chainId).web3j
        val transactionManager = ReadonlyTransactionManager(web3j, address)
        val contract = IIssuerCommon.load(issuerAddress, web3j, transactionManager, DefaultGasProvider())
        return contract.isWalletApproved(address).sendSafely() ?: false
    }

    @Throws(InternalException::class)
    override fun sendFaucetFunds(addresses: List<String>, chainId: Long): String? {
        logger.info { "Sending funds to addresses: $addresses on chain: $chainId" }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainId)
        val faucet = blockchainProperties.faucet ?: throw InternalException(
            errorCode = ErrorCode.BLOCKCHAIN_CONFIG_MISSING,
            exceptionMessage = "Missing or disabled faucet configuration for chainId: $chainId"
        )

        val gasPrice = getGasPrice(chainId)
        logger.debug { "Gas price: $gasPrice" }

        val transactionManager = RawTransactionManager(
            blockchainProperties.web3j,
            faucet.credentials,
            chainId
        )
        val gasProvider = StaticGasProvider(gasPrice, applicationProperties.faucet.gasLimit)
        val faucetContract = IFaucetService.load(
            faucet.contractAddress,
            blockchainProperties.web3j,
            transactionManager,
            gasProvider
        )

        val sentTransaction = faucetContract.faucet(addresses).sendSafely()

        logger.info { "Successfully send request to fund addresses: $addresses on chain: $chainId" }

        return sentTransaction?.transactionHash
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(InternalException::class)
    override fun getAutoInvestStatus(records: List<InvestmentRecord>, chainId: Long): List<InvestmentRecordStatus> {
        val chainProperties = chainHandler.getBlockchainProperties(chainId)
        val web3j = chainProperties.web3j
        val autoInvest = chainProperties.autoInvest ?: throw InternalException(
            errorCode = ErrorCode.BLOCKCHAIN_CONFIG_MISSING,
            exceptionMessage = "Missing or disabled auto-invest configuration for chainId: $chainId"
        )
        val transactionManager = ReadonlyTransactionManager(web3j, autoInvest.credentials.address)
        val contract = IInvestService.load(
            autoInvest.contractAddress,
            web3j,
            transactionManager,
            DefaultGasProvider()
        )
        val result = contract.getStatus(records).sendSafely() as List<InvestmentRecordStatus>?
        return result ?: emptyList()
    }

    @Throws(InternalException::class)
    override fun autoInvestFor(records: List<InvestmentRecordStatus>, chainId: Long): String? {
        logger.info { "Auto-investing on chainId: $chainId" }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainId)
        val autoInvest = blockchainProperties.autoInvest ?: throw InternalException(
            errorCode = ErrorCode.BLOCKCHAIN_CONFIG_MISSING,
            exceptionMessage = "Missing or disabled auto-invest configuration for chainId: $chainId"
        )

        val gasPrice = getGasPrice(chainId)
        logger.debug { "Gas price: $gasPrice" }

        val transactionManager = RawTransactionManager(
            blockchainProperties.web3j,
            autoInvest.credentials,
            chainId
        )
        val gasProvider = StaticGasProvider(gasPrice, applicationProperties.autoInvest.gasLimit)
        val autoInvestContract = IInvestService.load(
            autoInvest.contractAddress,
            blockchainProperties.web3j,
            transactionManager,
            gasProvider
        )

        val investmentRecords = records.map { InvestmentRecord(it) }
        val sentTransaction = autoInvestContract.investFor(investmentRecords).sendSafely()

        logger.info { "Successfully send request to auto-invest on chain: $chainId" }

        return sentTransaction?.transactionHash
    }

    @Throws(InternalException::class)
    override fun getContractVersion(chainId: Long, address: String): String? {
        val web3j = chainHandler.getBlockchainProperties(chainId).web3j
        val transactionManager = ReadonlyTransactionManager(web3j, address)
        val contract = IVersioned.load(address, web3j, transactionManager, DefaultGasProvider())
        return contract.version()?.sendSafely()
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
