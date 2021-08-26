package com.ampnet.identityservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "com.ampnet.identityservice")
class ApplicationProperties {
    val jwt = JwtProperties()
    val veriff = VeriffProperties()
    val test = TestProperties()
    val mail = MailProperties()
    val queue = QueueProperties()
    val chainEthereum = ChainProperties()
    val chainMatic = ChainProperties()
    val chainMumbai = ChainProperties()
    val chainHardhatTestnet = ChainProperties()
    val pinata = PinataProperties()
    lateinit var infuraId: String
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

class VeriffProperties {
    lateinit var apiKey: String
    lateinit var privateKey: String
    var baseUrl: String = "https://stationapi.veriff.com"
}

class TestProperties {
    var enabledTestKyc: Boolean = true
}

class ChainProperties {
    var privateKey: String = ""
    var walletApproverServiceAddress: String = ""
}

class MailProperties {
    var baseUrl: String = ""
    var sender: String = "no-reply@ampnet.io"
    var enabled: Boolean = false
}

@Suppress("MagicNumber")
class QueueProperties {
    var polling: Long = 5_000
    var initialDelay: Long = 15_000
    var miningPeriod: Long = 10 * 60 * 1000
}

@Suppress("MagicNumber")
class PinataProperties {
    var jwt: String = ""
    var maxUses: Int = 10
}
