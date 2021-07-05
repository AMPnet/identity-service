package com.ampnet.identityservice

import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
abstract class TestBase {

    protected fun suppose(@Suppress("UNUSED_PARAMETER") description: String, function: () -> Unit) {
        function.invoke()
    }

    protected fun verify(@Suppress("UNUSED_PARAMETER") description: String, function: () -> Unit) {
        function.invoke()
    }

    protected fun getResourceAsText(path: String) = this.javaClass.getResource(path).readText()
}
