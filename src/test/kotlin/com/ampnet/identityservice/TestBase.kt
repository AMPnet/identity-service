package com.ampnet.identityservice

import com.ampnet.identityservice.config.TestSchedulerConfiguration
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
}
