package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException
import com.ampnet.identityservice.exception.ResourceNotFoundException
import com.ampnet.identityservice.service.VerificationService
import com.ampnet.identityservice.util.ChainId
import com.ampnet.identityservice.util.ContractAddress
import com.ampnet.identityservice.util.WalletAddress
import mu.KLogging
import org.kethereum.crypto.signedMessageToKey
import org.kethereum.crypto.toAddress
import org.kethereum.model.SignatureData
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.security.SecureRandom
import java.security.SignatureException
import org.web3j.crypto.Hash.sha3 as keccak256

@Service
class VerificationServiceImpl(private val blockchainService: BlockchainService) : VerificationService {

    companion object : KLogging()

    @Suppress("MagicNumber")
    private val vOffset = BigInteger.valueOf(27L)
    private val userPayload = mutableMapOf<WalletAddress, String>()

    override fun generatePayload(address: WalletAddress): String {
        val nonce = SecureRandom().nextInt(Integer.MAX_VALUE).toString()
        val userMessage = SignMessage(address.value, nonce).toString()
        userPayload[address] = userMessage
        return userMessage
    }

    @Throws(ResourceNotFoundException::class, InvalidRequestException::class)
    override fun verifyPayload(address: WalletAddress, signedPayload: String) {
        val payload = userPayload[address] ?: throw ResourceNotFoundException(
            ErrorCode.AUTH_PAYLOAD_MISSING, "There is no payload associated with address: $address."
        )
        if (payload.length == 132) {
            verifyPersonalSignedPayload(address, payload, signedPayload)
        } else {
            verifyEip1271Signature(ChainId(0L), ContractAddress(address.value), payload, signedPayload)
        }
    }

    internal fun verifyEip1271Signature(chainId: ChainId, address: ContractAddress, payload: String, signedPayload: String) {
        val message = if (signedPayload == "0x") {
            generateEip191Message(payload.toByteArray())
        } else {
            payload.toByteArray()
        }
        val data = keccak256(message)
        if (blockchainService.isSignatureValid(chainId, address, data, signedPayload.toByteArray())) {
            throw InvalidRequestException(
                ErrorCode.AUTH_SIGNED_PAYLOAD_INVALID,
                "Contract: $address didn't sign the message: $signedPayload"
            )
        }
    }

    internal fun verifyPersonalSignedPayload(address: WalletAddress, payload: String, signedPayload: String) {
        val eip191 = generateEip191Message(payload.toByteArray())
        try {
            val signatureData = getPersonalSignatureData(signedPayload)
            val publicKey = signedMessageToKey(eip191, signatureData)
            if (address != WalletAddress(publicKey.toAddress().toString())) {
                throw InvalidRequestException(
                    ErrorCode.AUTH_SIGNED_PAYLOAD_INVALID,
                    "Address: $address not equal to signed address: ${publicKey.toAddress()}"
                )
            }
            userPayload.remove(address)
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
    private fun getPersonalSignatureData(signedPayload: String): SignatureData {
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

    private fun BigInteger.withVOffset(): BigInteger =
        if (this == BigInteger.ZERO || this == BigInteger.ONE) {
            this + vOffset
        } else {
            this
        }

    private fun Byte.toByteArray() = ByteArray(1) { this }

    private data class SignMessage(val address: String, val nonce: String) {
        override fun toString(): String =
            "Welcome!\nPlease sign this message to verify that you are the owner of address: $address\nNonce: $nonce"
    }
}
