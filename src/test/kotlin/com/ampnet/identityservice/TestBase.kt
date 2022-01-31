package com.ampnet.identityservice

import com.ampnet.identityservice.config.TestSchedulerConfiguration
import com.ampnet.identityservice.util.ChainId
import com.ampnet.identityservice.util.ContractAddress
import com.ampnet.identityservice.util.TransactionHash
import com.ampnet.identityservice.util.WalletAddress
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@Import(TestSchedulerConfiguration::class)
abstract class TestBase {

    protected fun suppose(@Suppress("UNUSED_PARAMETER") description: String, function: () -> Unit) {
        function.invoke()
    }

    protected fun verify(@Suppress("UNUSED_PARAMETER") description: String, function: () -> Unit) {
        function.invoke()
    }

    protected fun getResourceAsText(path: String) = this.javaClass.getResource(path).readText()

    // Fixes for Mockito (Mockito-Kotlin), because they STILL don't support value classes... in 2022... why even bother
    // maintaining a mocking library if you don't intent to support new language features?
    // https://github.com/mockito/mockito-kotlin/issues/309
    protected inline fun <reified T : Any> anyValueClass(unitValue: T): T {
        argThat<T> { true }
        return unitValue
    }

    protected fun TransactionHash.mockito(): TransactionHash {
        eq(value)
        return this
    }

    protected fun WalletAddress.mockito(): WalletAddress {
        eq(value)
        return this
    }

    protected fun ContractAddress.mockito(): ContractAddress {
        eq(value)
        return this
    }

    protected fun ChainId.mockito(): ChainId {
        eq(value)
        return this
    }
}
