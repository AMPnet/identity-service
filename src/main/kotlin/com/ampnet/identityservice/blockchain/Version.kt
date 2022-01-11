package com.ampnet.identityservice.blockchain

import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException

data class Version(val version: String) : Comparable<Version> {

    private val parts = version.split('.').mapNotNull { it.toIntOrNull() }

    fun requireSize(size: Int): Version {
        if (parts.size != size) {
            throw InvalidRequestException(ErrorCode.BLOCKCHAIN_UNSUPPORTED_VERSION, "Invalid contract version")
        }
        return this
    }

    override fun compareTo(other: Version): Int {
        tailrec fun compareVersion(index: Int, maxIndex: Int, other: Version): Int {
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
