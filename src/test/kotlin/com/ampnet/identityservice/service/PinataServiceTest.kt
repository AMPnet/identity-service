package com.ampnet.identityservice.service

import com.ampnet.identityservice.service.impl.PinataServiceImpl
import com.ampnet.identityservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

@Disabled("Not for automated testing")
@ActiveProfiles("secret")
class PinataServiceTest : JpaServiceTestBase() {

    @Test
    fun mustGetPinataJwt() {
        val service = PinataServiceImpl(applicationProperties, restTemplate)
        val jwt = service.getUserJwt(WalletAddress("test-api"))
        assertThat(jwt).isNotNull
    }
}
