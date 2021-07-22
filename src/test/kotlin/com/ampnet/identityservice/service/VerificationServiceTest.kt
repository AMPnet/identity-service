package com.ampnet.identityservice.service

import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException
import com.ampnet.identityservice.service.impl.VerificationServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kethereum.crypto.createEthereumKeyPair
import org.kethereum.crypto.signMessage
import org.kethereum.crypto.test_data.ADDRESS
import org.kethereum.crypto.test_data.KEY_PAIR
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toHex

class VerificationServiceTest : JpaServiceTestBase() {

    private val verificationService = VerificationServiceImpl()

    @Test
    fun mustBeAbleToVerifyPayload() {
        val address = ADDRESS.toString()
        val payload = verificationService.generatePayload(address)
        val signedPayload = "0x" + KEY_PAIR.signMessage(payload.toByteArray()).toHex()
        verificationService.verifyPayload(address, signedPayload)
    }

    @Test
    fun mustSignWithRandomKeyPair() {
        val keyPair = createEthereumKeyPair()
        val address = keyPair.publicKey.toAddress().hex
        val payload = verificationService.generatePayload(address)
        val signedPayload = keyPair.signMessage(payload.toByteArray()).toHex()
        verificationService.verifyPayload(address, "0x$signedPayload")
    }

    @Test
    fun mustFailOnInvalidSignature() {
        verificationService.generatePayload(ADDRESS.toString())
        val signedPayload = "0xb2c945a6cec73f6fac442eef9a59f9c35af728211b974167f581fc61954749e25259adb2034cdd15241ead0e6e9e7524c2f2126f02c0404a7c9403cec4b99dc01b"
        val error = assertThrows<InvalidRequestException> {
            assertThat(verificationService.verifyPayload(ADDRESS.toString(), signedPayload))
        }
        assertThat(error.errorCode).isEqualTo(ErrorCode.AUTH_SIGNED_PAYLOAD_INVALID)
    }
}
