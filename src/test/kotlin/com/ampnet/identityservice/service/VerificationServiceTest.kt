package com.ampnet.identityservice.service

import com.ampnet.identityservice.blockchain.BlockchainServiceImpl
import com.ampnet.identityservice.blockchain.properties.Chain
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException
import com.ampnet.identityservice.service.impl.VerificationServiceImpl
import com.ampnet.identityservice.util.ContractAddress
import com.ampnet.identityservice.util.WalletAddress
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

    private val blockchainService by lazy { BlockchainServiceImpl(applicationProperties, restTemplate) }
    private val verificationService by lazy { VerificationServiceImpl(blockchainService) }

    @Test
    fun mustBeAbleToVerifyPayload() {
        val address = WalletAddress(ADDRESS.toString())
        val payload = verificationService.generatePayload(address)
        val signedPayload = "0x" + KEY_PAIR.signWithEIP191PersonalSign(payload.toByteArray()).toHex()
        verificationService.verifyPayload(address, signedPayload)
    }

    @Test
    fun mustSignWithRandomKeyPair() {
        val keyPair = createEthereumKeyPair()
        val address = WalletAddress(keyPair.publicKey.toAddress().toString())
        val payload = verificationService.generatePayload(address)
        val signedPayload = keyPair.signWithEIP191PersonalSign(payload.toByteArray()).toHex()
        verificationService.verifyPayload(address, "0x$signedPayload")
    }

    @Test
    fun mustValidateCustomSignature() {
        val customSignature =
            "0x3d93d92c4b7f7c99857a004c7a40f24b19629c8b6904ff4ab855219577ad07951f42813cf161b9aeb77d7e8183bbf565ac065a" +
                "cb7c258db348d08932f8768c541b"
        val private =
            PrivateKey(decode("12d7b2a19c6b9ef7d485152e01c0d61551c0bf10bbe520374a2abe3445fcb405")).toECKeyPair()
        val keyPair = ECKeyPair(private.privateKey, private.publicKey)
        val address = WalletAddress(keyPair.publicKey.toAddress().toString())
        val payload = "4305308538901004665"
        val mySigned = "0x" + keyPair.signWithEIP191PersonalSign(payload.toByteArray()).toHex()
        assertThat(mySigned).isEqualTo(customSignature)
        verificationService.verifyPersonalSignedPayload(address, payload, mySigned)
    }

    @Test
    fun mustValidateCustomSignatureWithInternalVValue() {
        val customSignature = "0x3d93d92c4b7f7c99857a004c7a40f24b19629c8b6904ff4ab855219577ad07951f42813cf161b9aeb77d" +
            "7e8183bbf565ac065acb7c258db348d08932f8768c5400"
        val private = PrivateKey(decode("12d7b2a19c6b9ef7d485152e01c0d61551c0bf10bbe520374a2abe3445fcb405"))
            .toECKeyPair()
        val keyPair = ECKeyPair(private.privateKey, private.publicKey)
        val address = WalletAddress(keyPair.publicKey.toAddress().toString())
        val payload = "4305308538901004665"
        val mySigned = "0x" + keyPair.signWithEIP191PersonalSign(payload.toByteArray()).toHex()
        assertThat(mySigned.replace("1b$".toRegex(), "00")).isEqualTo(customSignature)
        verificationService.verifyPersonalSignedPayload(address, payload, mySigned)
    }

    @Test
    fun mustFailOnInvalidSignature() {
        val address = WalletAddress(ADDRESS.toString())
        verificationService.generatePayload(address)
        val signedPayload = "0xb2c945a6cec73f6fac442eef9a59f9c35af728211b974167f581fc61954749e25259adb2034cdd15241ead" +
            "0e6e9e7524c2f2126f02c0404a7c9403cec4b99dc01b"
        val error = assertThrows<InvalidRequestException> {
            assertThat(verificationService.verifyPayload(address, signedPayload))
        }
        assertThat(error.errorCode).isEqualTo(ErrorCode.AUTH_SIGNED_PAYLOAD_INVALID)
    }

    @Test
//    @Disabled("Not for automated testing")
    fun mustValidatePersonalSignature() {
        val address = WalletAddress("0x9a72ad187229e9338c7f21e019544947fb25d473")
        val message =
            "Welcome!\nPlease sign this message to verify that you are the owner of address: 0x9a72ad187229e9338c7f21e019544947fb25d473\nNonce: 1202780025"
        val signature =
            "0xbc450b7d46c065c824af75c93b7baac89c8282ee1496474fd8707692ba1df8250de4dc5dc4cb770c39a04a6e9f9ebe55b7111f359ad24f79e1aa7a3f50620b841b"
        verificationService.verifyPersonalSignedPayload(address, message, signature)
    }

    @Test
//    @Disabled("Not for automated testing")
    fun mustValidateGnosisSignature() {
        val address = ContractAddress("0x6cf77b38c601c8c93271a9ea27ca4a3209b67ff3")
        val message =
            "Welcome!\nPlease sign this message to verify that you are the owner of address: 0x6cf77b38c601c8c93271a9ea27ca4a3209b67ff3\nNonce: 1709156313"
        val signature = "0x"
        verificationService.verifyEip1271Signature(Chain.MATIC_MAIN.id, address, message, signature)
    }

    @Test
//    @Disabled("Not for automated testing")
    fun mustValidateAmbireSignature() {
        val address = ContractAddress("0x5b8502e07d49e66d4273bf453f6f033163d3b4e8")
        val message =
            "Welcome!\nPlease sign this message to verify that you are the owner of address: 0x5b8502e07d49e66d4273bf453f6f033163d3b4e8\nNonce: 1561938464"
        val signature =
            "0x000000000000000000000000000000000000000000000000000000000003f480000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000e000000000000000000000000000000000000000000000000000000000000000427a809a26e054a7c1a8093a2edbc548e0aabaa35bfa3081d5abf6253534351dac76b32447c40671984794f0e7a42052aa975e464bd9bd001cb4a21c90a4ef7c951b01000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004203d4c7efd3c1040178602a78e2cda8e2205b8cde98316ddecb027cda52f5da5b7e96107503d31ac8c903a3f8229fb08bb189ec1f149ac3fe5b6f8373c91e17f51c01000000000000000000000000000000000000000000000000000000000000000000000000000000000000ff3f6d14df43c112ab98834ee1f82083e07c26bf02"
        verificationService.verifyEip1271Signature(Chain.MATIC_MAIN.id, address, message, signature)
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
