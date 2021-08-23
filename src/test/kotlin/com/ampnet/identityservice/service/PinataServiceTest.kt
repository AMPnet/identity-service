package com.ampnet.identityservice.service

import com.ampnet.identityservice.service.impl.PinataServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("secret")
class PinataServiceTest : JpaServiceTestBase() {

    @Test
    fun mustGetPinataJwt() {
        val service = PinataServiceImpl(applicationProperties, restTemplate)
        val jwt = service.getUserJwt("test-api")
        assertThat(jwt).isNotNull
    }
}
