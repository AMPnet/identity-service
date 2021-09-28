package com.ampnet.identityservice.service

import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException
import com.ampnet.identityservice.service.impl.VerificationServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kethereum.crypto.createEthereumKeyPair
import org.kethereum.crypto.test_data.ADDRESS
import org.kethereum.crypto.test_data.KEY_PAIR
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toECKeyPair
import org.kethereum.crypto.toHex
import org.kethereum.eip191.signWithEIP191PersonalSign
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PrivateKey

class VerificationServiceTest : JpaServiceTestBase() {

    private val verificationService = VerificationServiceImpl()

    @Test
    fun mustBeAbleToVerifyPayload() {
        val address = ADDRESS.toString()
        val payload = verificationService.generatePayload(address)
        val signedPayload = "0x" + KEY_PAIR.signWithEIP191PersonalSign(payload.toByteArray()).toHex()
        verificationService.verifyPayload(address, signedPayload)
    }

    @Test
    fun mustSignWithRandomKeyPair() {
        val keyPair = createEthereumKeyPair()
        val address = keyPair.publicKey.toAddress().hex
        val payload = verificationService.generatePayload(address)
        val signedPayload = keyPair.signWithEIP191PersonalSign(payload.toByteArray()).toHex()
        verificationService.verifyPayload(address, "0x$signedPayload")
    }

    @Test
    fun mustValidateCustomSignature() {
        val customSignature = "0x3d93d92c4b7f7c99857a004c7a40f24b19629c8b6904ff4ab855219577ad07951f42813cf161b9aeb77d7e8183bbf565ac065acb7c258db348d08932f8768c541b"
        val private = PrivateKey(decode("12d7b2a19c6b9ef7d485152e01c0d61551c0bf10bbe520374a2abe3445fcb405")).toECKeyPair()
        val keyPair = ECKeyPair(private.privateKey, private.publicKey)
        val address = keyPair.publicKey.toAddress().hex
        val payload = "4305308538901004665"
        val mySigned = "0x" + keyPair.signWithEIP191PersonalSign(payload.toByteArray()).toHex()
        assertThat(mySigned).isEqualTo(customSignature)
        verificationService.verifySignedPayload(address, payload, mySigned)
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

    private fun decode(value: String): ByteArray {
        // An hex string must always have length multiple of 2
        if (value.length % 2 != 0) {
            throw IllegalArgumentException("hex-string must have an even number of digits (nibbles)")
        }

        // Remove the 0x prefix if it is set
        val cleanInput = if (value.startsWith("0x")) value.substring(2) else value

        return ByteArray(cleanInput.length / 2).apply {
            var i = 0
            while (i < cleanInput.length) {
                this[i / 2] = (
                    (hexToBin(cleanInput[i]) shl 4) + hexToBin(
                        cleanInput[i + 1]
                    )
                    ).toByte()
                i += 2
            }
        }
    }
    private fun hexToBin(ch: Char): Int = when (ch) {
        in '0'..'9' -> ch - '0'
        in 'A'..'F' -> ch - 'A' + 10
        in 'a'..'f' -> ch - 'a' + 10
        else -> throw(IllegalArgumentException("'$ch' is not a valid hex character"))
    }
}
