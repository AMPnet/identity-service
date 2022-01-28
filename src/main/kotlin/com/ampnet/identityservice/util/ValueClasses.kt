package com.ampnet.identityservice.util

import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException

@JvmInline
value class TransactionHash(val value: String)

@JvmInline
value class WalletAddress private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = WalletAddress(value.lowercase())
    }
}

@JvmInline
value class ContractAddress private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = ContractAddress(value.lowercase())
    }
}

data class ContractVersion(val value: String) : Comparable<ContractVersion> {

    private val parts = value.split('.').mapNotNull { it.toIntOrNull() }

    fun requireSize(size: Int): ContractVersion {
        if (parts.size != size) {
            throw InvalidRequestException(ErrorCode.BLOCKCHAIN_UNSUPPORTED_VERSION, "Invalid contract version")
        }
        return this
    }

    override fun compareTo(other: ContractVersion): Int {
        tailrec fun compareVersion(index: Int, maxIndex: Int, other: ContractVersion): Int {
            val thisPart = parts.elementAtOrElse(index) { 0 }
            val thatPart = other.parts.elementAtOrElse(index) { 0 }
            val result = thisPart.compareTo(thatPart)

            return if (result != 0 || index >= maxIndex) {
                result
            } else {
                compareVersion(index + 1, maxIndex, other)
            }
        }

        val maxIndex = Integer.max(parts.size, other.parts.size)
        return compareVersion(0, maxIndex, other)
    }
}

@JvmInline
value class ChainId(val value: Long)
