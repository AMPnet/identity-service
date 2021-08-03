package com.ampnet.identityservice.service

import org.junit.jupiter.api.Test
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService

class BlockchainServiceTest {

    private val web3j = Web3j.build(HttpService("https://polygon-mumbai.infura.io/v3/d589ee21dd964259829d2f76f02ac8b1"))
    private val credentials = Credentials.create("5ee3ec46a8d3d31c1cd0f9cf1c03770e03dea133b1ccdc25dac0eac7a9b09144")

    @Test
    fun mustGetNonce() {
        println("Fetching nonce")
        val nonce = web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.LATEST)
            .send().transactionCount
        println("Nonce is: $nonce")
    }
}
