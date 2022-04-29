package com.ampnet.identityservice.blockchain

import com.ampnet.identityservice.blockchain.IInvestService.InvestmentRecord
import com.ampnet.identityservice.blockchain.IInvestService.InvestmentRecordStatus
import com.ampnet.identityservice.blockchain.properties.ChainPropertiesHandler
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InternalException
import com.ampnet.identityservice.util.ChainId
import com.ampnet.identityservice.util.ContractAddress
import com.ampnet.identityservice.util.ContractVersion
import com.ampnet.identityservice.util.TransactionHash
import com.ampnet.identityservice.util.WalletAddress
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.tx.gas.StaticGasProvider
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.io.IOException
import java.math.BigInteger

private val logger = KotlinLogging.logger {}

@Service
class BlockchainServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val restTemplate: RestTemplate
) : BlockchainService {

    private val chainHandler = ChainPropertiesHandler(applicationProperties)
    private val validSignature = Numeric.hexStringToByteArray("0x1626ba7e")

    @Suppress("ReturnCount")
    @Throws(InternalException::class)
    override fun whitelistAddresses(
        addresses: List<WalletAddress>,
        issuerAddress: ContractAddress,
        chainId: ChainId
    ): TransactionHash? {
        logger.info { "Whitelisting addresses: $addresses on chain: $chainId for issuer: $issuerAddress" }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainId)
        val gasPrice = getGasPrice(chainId)
        logger.debug { "Gas price: $gasPrice" }

        val transactionManager = LatestRawTransactionManager(
            blockchainProperties.web3j,
            blockchainProperties.walletApprover.credentials,
            chainId.value,
            blockchainProperties.blockTime
        )
        val gasProvider = StaticGasProvider(gasPrice, applicationProperties.walletApprove.gasLimit)
        val walletApproverContract = WalletApproverService.load(
            blockchainProperties.walletApprover.contractAddress.value,
            blockchainProperties.web3j,
            transactionManager,
            gasProvider
        )

        val sentTransaction = walletApproverContract.approveWallets(
            issuerAddress.value,
            addresses.map { it.value }
        ).sendSafely()

        logger.info {
            "Successfully send request to whitelist addresses: $addresses on chain: $chainId for issuer: $issuerAddress"
        }

        return sentTransaction?.transactionHash?.let { TransactionHash(it) }
    }

    @Throws(InternalException::class)
    override fun isMined(hash: TransactionHash, chainId: ChainId): Boolean {
        val web3j = chainHandler.getBlockchainProperties(chainId).web3j
        val transaction = web3j.ethGetTransactionReceipt(hash.value).sendSafely()
        return transaction?.transactionReceipt?.isPresent ?: false
    }

    @Throws(InternalException::class)
    override fun isWhitelisted(address: WalletAddress, issuerAddress: ContractAddress?, chainId: ChainId): Boolean {
        if (issuerAddress == null)
            return false
        val web3j = chainHandler.getBlockchainProperties(chainId).web3j
        val transactionManager = ReadonlyTransactionManager(web3j, address.value)
        val contract = IIssuerCommon.load(issuerAddress.value, web3j, transactionManager, DefaultGasProvider())
        return contract.isWalletApproved(address.value).sendSafely() ?: false
    }

    @Throws(InternalException::class)
    override fun sendFaucetFunds(addresses: List<WalletAddress>, chainId: ChainId): TransactionHash? {
        logger.info { "Sending funds to addresses: $addresses on chain: $chainId" }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainId)
        val faucet = blockchainProperties.faucet ?: throw InternalException(
            errorCode = ErrorCode.BLOCKCHAIN_CONFIG_MISSING,
            exceptionMessage = "Missing or disabled faucet configuration for chainId: $chainId"
        )

        val gasPrice = getGasPrice(chainId, true)
        logger.debug { "Gas price: $gasPrice" }

        val transactionManager = LatestRawTransactionManager(
            blockchainProperties.web3j,
            faucet.credentials,
            chainId.value,
            blockchainProperties.blockTime
        )
        val gasProvider = StaticGasProvider(gasPrice, applicationProperties.faucet.gasLimit)
        val faucetContract = IFaucetService.load(
            faucet.contractAddress.value,
            blockchainProperties.web3j,
            transactionManager,
            gasProvider
        )

        val sentTransaction = faucetContract.faucet(addresses.map { it.value }).sendSafely()

        logger.info { "Successfully send request to fund addresses: $addresses on chain: $chainId" }

        return sentTransaction?.transactionHash?.let { TransactionHash(it) }
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(InternalException::class)
    override fun getAutoInvestStatus(records: List<InvestmentRecord>, chainId: ChainId): List<InvestmentRecordStatus> {
        val chainProperties = chainHandler.getBlockchainProperties(chainId)
        val web3j = chainProperties.web3j
        val autoInvest = chainProperties.autoInvest ?: throw InternalException(
            errorCode = ErrorCode.BLOCKCHAIN_CONFIG_MISSING,
            exceptionMessage = "Missing or disabled auto-invest configuration for chainId: $chainId"
        )
        val transactionManager = ReadonlyTransactionManager(web3j, autoInvest.credentials.address)
        val contract = IInvestService.load(
            autoInvest.contractAddress.value,
            web3j,
            transactionManager,
            DefaultGasProvider()
        )
        val result = contract.getStatus(records).sendSafely() as List<InvestmentRecordStatus>?
        logger.debug {
            "Received auto-invest statues: ${result?.joinToString {
                "[investor: ${it.investor}, amount: ${it.amount}, campaign: ${it.campaign}, ready: ${it.readyToInvest}]"
            }}"
        }
        return result ?: emptyList()
    }

    @Throws(InternalException::class)
    override fun autoInvestFor(records: List<InvestmentRecordStatus>, chainId: ChainId): TransactionHash? {
        logger.info { "Auto-investing on chainId: $chainId" }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainId)
        val autoInvest = blockchainProperties.autoInvest ?: throw InternalException(
            errorCode = ErrorCode.BLOCKCHAIN_CONFIG_MISSING,
            exceptionMessage = "Missing or disabled auto-invest configuration for chainId: $chainId"
        )

        val gasPrice = getGasPrice(chainId)
        logger.debug { "Gas price: $gasPrice" }

        val transactionManager = LatestRawTransactionManager(
            blockchainProperties.web3j,
            autoInvest.credentials,
            chainId.value,
            blockchainProperties.blockTime
        )
        val gasProvider = StaticGasProvider(gasPrice, applicationProperties.autoInvest.gasLimit)
        val autoInvestContract = IInvestService.load(
            autoInvest.contractAddress.value,
            blockchainProperties.web3j,
            transactionManager,
            gasProvider
        )

        val investmentRecords = records.map { InvestmentRecord(it) }
        logger.info {
            "Auto-investing for: ${investmentRecords.joinToString {
                "[investor: ${it.investor}, amount: ${it.amount}, campaign: ${it.campaign}]"
            }}"
        }
        val sentTransaction = autoInvestContract.investFor(investmentRecords).sendSafely()

        logger.info { "Successfully send request to auto-invest on chain: $chainId" }

        return sentTransaction?.transactionHash?.let { TransactionHash(it) }
    }

    @Throws(InternalException::class)
    override fun getContractVersion(chainId: ChainId, address: ContractAddress): ContractVersion? {
        val web3j = chainHandler.getBlockchainProperties(chainId).web3j
        val transactionManager = ReadonlyTransactionManager(web3j, address.value)
        val contract = IVersioned.load(address.value, web3j, transactionManager, DefaultGasProvider())
        return contract.version()?.sendSafely()?.let { ContractVersion(it) }
    }

    @Throws(InternalException::class)
    override fun isSignatureValid(
        chainId: ChainId,
        address: ContractAddress,
        data: ByteArray,
        signature: ByteArray
    ): Boolean {
        val web3j = chainHandler.getBlockchainProperties(chainId).web3j
        val transactionManager = ReadonlyTransactionManager(web3j, address.value)
        val contract = IERC1271.load(address.value, web3j, transactionManager, DefaultGasProvider())
        return contract.isValidSignature(data, signature)?.sendSafely()
            ?.let { it.contentEquals(validSignature) }
            ?: false
    }

    @Suppress("NestedBlockDepth")
    internal fun getGasPrice(chainId: ChainId, fastest: Boolean = false): BigInteger? {
        chainHandler.getGasPriceFeed(chainId)?.let { url ->
            try {
                val response = restTemplate
                    .getForObject<GasPriceFeedResponse>(url, GasPriceFeedResponse::class)
                return if (fastest) {
                    response.fastest?.let { price ->
                        @Suppress("MagicNumber")
                        val wei = Convert.toWei((price * 1.2).toBigDecimal(), Convert.Unit.GWEI).toBigInteger()
                        logger.debug { "Fetched gas price in Wei: $wei" }
                        wei
                    }
                } else {
                    response.fast?.let { price ->
                        val wei = Convert.toWei(price.toBigDecimal(), Convert.Unit.GWEI).toBigInteger()
                        logger.debug { "Fetched gas price in Wei: $wei" }
                        wei
                    }
                }
            } catch (ex: RestClientException) {
                logger.warn { "Failed to get price for feed: $url" }
            }
        }
        return chainHandler.getBlockchainProperties(chainId)
            .web3j.ethGasPrice().sendSafely()?.gasPrice
    }

    private data class GasPriceFeedResponse(
        val safeLow: Double?,
        val standard: Double?,
        val fast: Double?,
        val fastest: Double?,
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
