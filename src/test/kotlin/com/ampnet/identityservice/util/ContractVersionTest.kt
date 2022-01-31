package com.ampnet.identityservice.util

import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ContractVersionTest {

    @Test
    fun mustCompareVersion() {
        assertTrue(ContractVersion("2") > ContractVersion("1"))
        assertTrue(ContractVersion("2") > ContractVersion("1.0"))
        assertTrue(ContractVersion("1.0.1") > ContractVersion("1.0"))
        assertTrue(ContractVersion("1.2") > ContractVersion("1.0.1"))
        assertTrue(ContractVersion("1.0.1") > ContractVersion("1.0.0"))
    }

    @Test
    fun mustThrowExceptionForInvalidVersionSize() {
        val exception = assertThrows<InvalidRequestException> {
            ContractVersion("2").requireSize(2)
        }
        assertThat(exception.errorCode).isEqualTo(ErrorCode.BLOCKCHAIN_UNSUPPORTED_VERSION)
    }
}
