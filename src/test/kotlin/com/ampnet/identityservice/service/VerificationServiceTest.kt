package com.ampnet.identityservice.service

import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.service.impl.VerificationServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kethereum.crypto.signMessage
import org.kethereum.crypto.test_data.ADDRESS
import org.kethereum.crypto.test_data.KEY_PAIR
import org.kethereum.crypto.toHex
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(value = [SpringExtension::class])
class VerificationServiceTest : TestBase() {

    private val verificationService = VerificationServiceImpl()

    @Test
    fun mustBeAbleToVerifyPayload() {
        val payload = verificationService.generatePayload(ADDRESS.toString())
        val signedPayload = "0x" + KEY_PAIR.signMessage(payload.toByteArray()).toHex()
        assertThat(verificationService.verifyPayload(ADDRESS.toString(), signedPayload)).isTrue
    }
}
