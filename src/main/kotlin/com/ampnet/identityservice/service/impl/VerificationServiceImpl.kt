package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException
import com.ampnet.identityservice.exception.ResourceNotFoundException
import com.ampnet.identityservice.service.VerificationService
import mu.KLogging
import org.kethereum.crypto.signedMessageToKey
import org.kethereum.crypto.toAddress
import org.kethereum.model.SignatureData
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.security.SecureRandom
import java.security.SignatureException

@Service
class VerificationServiceImpl : VerificationService {

    companion object : KLogging()

    private val userPayload = mutableMapOf<String, String>()
    private val message = "Welcome!\nClick “Sign” to sign in. No password needed!\nNonce: "
    private val vOffset = BigInteger.valueOf(27L)

    override fun generatePayload(address: String): String {
        val nonce = SecureRandom().nextInt(Integer.MAX_VALUE).toString()
        val userMessage = message + nonce
        userPayload[address.lowercase()] = userMessage
        return userMessage
    }

    @Throws(ResourceNotFoundException::class, InvalidRequestException::class)
    override fun verifyPayload(address: String, signedPayload: String) {
        val lowercaseAddress = address.lowercase()
        val payload = userPayload[lowercaseAddress] ?: throw ResourceNotFoundException(
            ErrorCode.AUTH_PAYLOAD_MISSING, "There is no payload associated with address: $lowercaseAddress."
        )
        verifySignedPayload(lowercaseAddress, payload, signedPayload)
    }

    internal fun verifySignedPayload(address: String, payload: String, signedPayload: String) {
        val lowercaseAddress = address.lowercase()
        val eip919 = generateEip191Message(payload.toByteArray())
        try {
            val signatureData = getSignatureData(signedPayload)
            val publicKey = signedMessageToKey(eip919, signatureData)
            if (lowercaseAddress != publicKey.toAddress().toString().lowercase()) {
                throw InvalidRequestException(
                    ErrorCode.AUTH_SIGNED_PAYLOAD_INVALID,
                    "Address: $lowercaseAddress not equal to signed address: ${publicKey.toAddress()}"
                )
            }
            userPayload.remove(lowercaseAddress)
        } catch (ex: SignatureException) {
            throw InvalidRequestException(
                ErrorCode.AUTH_SIGNED_PAYLOAD_INVALID, "Public key cannot be recovered from the signature", ex
            )
        }
    }

    private fun generateEip191Message(message: ByteArray): ByteArray =
        0x19.toByte().toByteArray() + ("Ethereum Signed Message:\n" + message.size).toByteArray() + message

    /*
     ECDSA signatures consist of two numbers(integers): r and s.
     Ethereum uses an additional v(recovery identifier) variable. The signature can be notated as {r, s, v}.
     */
    @Suppress("MagicNumber")
    private fun getSignatureData(signedPayload: String): SignatureData {
        if (signedPayload.length != 132)
            throw InvalidRequestException(
                ErrorCode.AUTH_SIGNED_PAYLOAD_INVALID, "Signature: $signedPayload is of wrong length."
            )
        val r = signedPayload.substring(2, 66)
        val s = signedPayload.substring(66, 130)
        val v = signedPayload.substring(130, 132)
        try {
            return SignatureData(BigInteger(r, 16), BigInteger(s, 16), BigInteger(v, 16).withVOffset())
        } catch (ex: NumberFormatException) {
            throw InvalidRequestException(
                ErrorCode.AUTH_SIGNED_PAYLOAD_INVALID, "Signature: $signedPayload is not a valid hex value.", ex
            )
        }
    }

    private fun BigInteger.withVOffset(): BigInteger {
        return if (this == BigInteger.ZERO || this == BigInteger.ONE) {
            this + vOffset
        } else {
            this
        }
    }
}

private fun Byte.toByteArray() = ByteArray(1) { this }
