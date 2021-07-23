package com.ampnet.identityservice.security

import org.springframework.security.test.context.support.WithSecurityContext

@Retention(value = AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@WithSecurityContext(factory = WithMockUserSecurityFactory::class)
annotation class WithMockCrowdfundUser(
    val address: String = "0xef678007d18427e6022059dbc264f27507cd1ffc"
)
