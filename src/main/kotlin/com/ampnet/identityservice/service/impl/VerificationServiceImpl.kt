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
import kotlin.jvm.Throws

@Service
class VerificationServiceImpl : VerificationService {

    companion object : KLogging()

    private val userPayload = mutableMapOf<String, String>()

    override fun generatePayload(address: String): String {
        // TODO generates a negative number, find a function in kethereum
        val nonce = SecureRandom().nextLong().toString()
        userPayload[address] = nonce
        return nonce
    }

    @Throws(ResourceNotFoundException::class, InvalidRequestException::class)
    override fun verifyPayload(address: String, signedPayload: String): Boolean {
        val payload = userPayload[address]
            ?: throw ResourceNotFoundException(
                ErrorCode.AUTH_PAYLOAD_MISSING, "There is no payload associated with address: $address."
            )
        userPayload.remove(address)
        try {
            val publicKey = signedMessageToKey(payload.toByteArray(), getSignatureData(signedPayload))
            return address == publicKey.toAddress().toString()
        } catch (ex: SignatureException) {
            throw InvalidRequestException(
                ErrorCode.AUTH_SIGNED_PAYLOAD_INVALID, "Public key cannot be recovered from the signature", ex
            )
        }
    }

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
            return SignatureData(BigInteger(r, 16), BigInteger(s, 16), BigInteger(v, 16))
        } catch (ex: NumberFormatException) {
            throw InvalidRequestException(
                ErrorCode.AUTH_SIGNED_PAYLOAD_INVALID, "Signature: $signedPayload is not a valid hex value.", ex
            )
        }
    }
}
