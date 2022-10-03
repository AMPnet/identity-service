package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.controller.pojo.request.AuthorizationRequestByMessage
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
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.security.SecureRandom
import java.security.SignatureException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.web3j.crypto.Hash.sha3 as keccak256

@Service
class VerificationServiceImpl(private val blockchainService: BlockchainService) : VerificationService {

    companion object : KLogging()

    @Suppress("MagicNumber")
    private val vOffset = BigInteger.valueOf(27L)

    @Suppress("MagicNumber")
    private val personalSignatureLength = 132
    private val userPayload = ConcurrentHashMap<WalletAddress, String>()
    private val messagePayloads = ConcurrentHashMap<String, String>()

    override fun generatePayload(address: WalletAddress): String {
        val nonce = SecureRandom().nextInt(Integer.MAX_VALUE).toString()
        val userMessage = SignMessage(address.value, nonce).toString()
        userPayload[address] = userMessage
        return userMessage
    }

    override fun generatePayloadByMessage(): String {
        val nonce = SecureRandom().nextInt(Integer.MAX_VALUE).toString()
        val userMessage = SignMessage(null, nonce).toString()
        messagePayloads[userMessage] = userMessage
        return userMessage
    }

    @Throws(ResourceNotFoundException::class, InvalidRequestException::class)
    override fun verifyPayload(address: WalletAddress, signedPayload: String, chainId: ChainId?) {
        val payload = userPayload[address] ?: throw ResourceNotFoundException(
            ErrorCode.AUTH_PAYLOAD_MISSING, "There is no payload associated with address: $address."
        )
        if (signedPayload.length == personalSignatureLength) {
            verifyPersonalSignedPayload(address, payload, signedPayload)
        } else {
            if (chainId == null) {
                throw InvalidRequestException(
                    ErrorCode.BLOCKCHAIN_ID,
                    "Cannot verify contract signature without chain id"
                )
            }
            verifyEip1271Signature(chainId, ContractAddress(address.value), payload, signedPayload)
        }
    }

    override fun verifyPayloadByMessage(request: AuthorizationRequestByMessage) {
        val payload = messagePayloads[request.messageToSign] ?: throw ResourceNotFoundException(
            ErrorCode.AUTH_PAYLOAD_MISSING, "There is no payload for message: ${request.messageToSign}."
        )

        return verifyMessageSignedPayload(WalletAddress(request.address), payload, request.signedPayload)
    }

    internal fun verifyEip1271Signature(
        chainId: ChainId,
        address: ContractAddress,
        payload: String,
        signedPayload: String
    ) {
        val message = if (signedPayload == "0x") {
            generateEip191Message(payload.toByteArray())
        } else {
            payload.toByteArray()
        }
        val encodedMessage = keccak256(message)
        val encodedSignedPayload = Numeric.hexStringToByteArray(signedPayload)
        if (!blockchainService.isSignatureValid(chainId, address, encodedMessage, encodedSignedPayload)) {
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

    internal fun verifyMessageSignedPayload(address: WalletAddress, payload: String, signedPayload: String) {
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
            messagePayloads.remove(payload)
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
        if (signedPayload.length != personalSignatureLength)
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

    private data class SignMessage(val address: String?, val nonce: String) {
        override fun toString(): String =
            if (address != null) {
                "Welcome!\nPlease sign this message to verify that you are the owner of address:" +
                    " $address\nNonce: $nonce"
            } else {
                "Please sign this message to verify your wallet ownership," +
                    " unique ID: ${UUID.randomUUID()}, nonce: $nonce"
            }
    }
}
