package com.ampnet.identityservice.service

import com.ampnet.identityservice.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.web3j.crypto.Keys

@ActiveProfiles("secret")
@SpringBootTest
class Web3jServiceTest : TestBase() {

    @Autowired
    private lateinit var web3jService: Web3jService

    @Autowired
    private lateinit var jobSchedulingService: JobSchedulingService

    @Test
    fun mustBeAbleToApproveWallets() {
        val addresses = mutableListOf<String>()
        suppose("There are three addresses whitelisted") {
            for (i in 0..1) {
                val keyPair = Keys.createEcKeyPair()
                val address = "0x" + Keys.getAddress(keyPair.publicKey)
                addresses.add(address)
            }
            addresses.forEach { jobSchedulingService.enqueueTransaction(it) }
        }

        verify("Wallet addresses are approved") {
            Thread.sleep(60000)
            addresses.forEach {
                val approved = web3jService.isWalletApproved(it)
                assertThat(approved).isTrue
            }
        }
    }
}
