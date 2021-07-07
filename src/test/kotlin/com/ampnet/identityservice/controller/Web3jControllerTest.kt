package com.ampnet.identityservice.controller

import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.contract.Issuer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.StaticGasProvider
import java.math.BigInteger

@ExtendWith(value = [SpringExtension::class])
class Web3jControllerTest {

    private val applicationProperties = ApplicationProperties()
    private val web3j = Web3j.build(HttpService(applicationProperties.provider.blockchainApi))

    private val privateKey = "034b74295a894357ad6e67c051009bf31de51a83df4c72751efbf7032b417ec2"
    private val contractAddress = "0x8Ba5082E853b87E8D92970506EBb7c7097d6F640"
    private val walletAddress = "0x745367860c5015B1E0AC04E00f1DbAd83B7dC272"

    @Test
    fun mustReturnTrueForWalletApproved() {
        val credentials = Credentials.create(privateKey)
        val contract = Issuer.load(
            contractAddress, web3j, credentials,
            StaticGasProvider(BigInteger("22000000000"), BigInteger("510000"))
        )
        contract.approveWallet(walletAddress).send()
        val isWalletApproved = contract.isWalletApproved(walletAddress).send()
        assertThat(isWalletApproved).isTrue
    }
}
