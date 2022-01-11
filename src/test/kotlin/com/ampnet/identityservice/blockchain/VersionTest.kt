package com.ampnet.identityservice.blockchain

import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VersionTest {

    @Test
    fun mustCompareVersion() {
        assertTrue(Version("2") > Version("1"))
        assertTrue(Version("2") > Version("1.0"))
        assertTrue(Version("1.0.1") > Version("1.0"))
        assertTrue(Version("1.2") > Version("1.0.1"))
        assertTrue(Version("1.0.1") > Version("1.0.0"))
    }

    @Test
    fun mustThrowExceptionForInvalidVersionSize() {
        val exception = assertThrows<InvalidRequestException> {
            Version("2").requireSize(2)
        }
        assertThat(exception.errorCode).isEqualTo(ErrorCode.BLOCKCHAIN_UNSUPPORTED_VERSION)
    }
}
