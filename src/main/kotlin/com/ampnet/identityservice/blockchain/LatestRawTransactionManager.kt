package com.ampnet.identityservice.blockchain

import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.RawTransactionManager
import java.io.IOException
import java.math.BigInteger

class LatestRawTransactionManager(
    private val web3j: Web3j,
    private val credentials: Credentials,
    chainId: Long,
    blockTime: Long?,
    attempts: Int = 5
) : RawTransactionManager(web3j, credentials, chainId, attempts, blockTime ?: DEFAULT_POLLING_FREQUENCY) {
    @Throws(IOException::class)
    override fun getNonce(): BigInteger {
        val ethGetTransactionCount =
            this.web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.LATEST).send()
        return ethGetTransactionCount.transactionCount
    }
}
