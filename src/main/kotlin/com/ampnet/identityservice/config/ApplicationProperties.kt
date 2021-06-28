package com.ampnet.identityservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "com.ampnet.identityservice")
class ApplicationProperties {
    val jwt: JwtProperties = JwtProperties()
}

@Suppress("MagicNumber")
class JwtProperties {
    lateinit var publicKey: String
    lateinit var privateKey: String
    var accessTokenValidityInMinutes: Long = 60 * 24
    var refreshTokenValidityInMinutes: Long = 60 * 24 * 90

    fun accessTokenValidityInMilliseconds(): Long = accessTokenValidityInMinutes * 60 * 1000
    fun refreshTokenValidityInMilliseconds(): Long = refreshTokenValidityInMinutes * 60 * 1000
}
