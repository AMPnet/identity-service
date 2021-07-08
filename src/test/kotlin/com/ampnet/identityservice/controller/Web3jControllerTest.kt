package com.ampnet.identityservice.controller

import com.ampnet.identityservice.contract.IIssuer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.StaticGasProvider
import java.math.BigInteger

@ExtendWith(value = [SpringExtension::class])
@ActiveProfiles("secret")
class Web3jControllerTest : ControllerTestBase() {

    private val web3j by lazy { Web3j.build(HttpService(applicationProperties.provider.blockchainApi)) }

    @Test
    fun mustReturnTrueForWalletApproved() {
        val credentials = Credentials.create(applicationProperties.smartContract.privateKey)
        val contract = IIssuer.load(
            applicationProperties.smartContract.issuerContractAddress, web3j, credentials,
            StaticGasProvider(BigInteger("22000000000"), BigInteger("510000"))
        )
        contract.approveWallet(applicationProperties.smartContract.walletAddress).send()
        val isWalletApproved = contract.isWalletApproved(applicationProperties.smartContract.walletAddress).send()
        assertThat(isWalletApproved).isTrue
    }
}
