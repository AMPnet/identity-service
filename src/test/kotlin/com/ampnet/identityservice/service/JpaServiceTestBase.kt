package com.ampnet.identityservice.service

import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.config.DatabaseCleanerService
import com.ampnet.identityservice.config.JsonConfig
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.persistence.repository.MailTokenRepository
import com.ampnet.identityservice.persistence.repository.UserInfoRepository
import com.ampnet.identityservice.persistence.repository.UserRepository
import com.ampnet.identityservice.persistence.repository.VeriffDecisionRepository
import com.ampnet.identityservice.persistence.repository.VeriffSessionRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.extension.ExtendWith
import org.kethereum.crypto.test_data.ADDRESS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.time.ZonedDateTime

@ExtendWith(SpringExtension::class)
@DataJpaTest
@Transactional
@Import(DatabaseCleanerService::class, ApplicationProperties::class, RestTemplate::class, JsonConfig::class)
abstract class JpaServiceTestBase : TestBase() {

    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService

    @Autowired
    protected lateinit var applicationProperties: ApplicationProperties

    @Autowired
    protected lateinit var restTemplate: RestTemplate

    @Autowired
    protected lateinit var veriffSessionRepository: VeriffSessionRepository

    @Autowired
    protected lateinit var veriffDecisionRepository: VeriffDecisionRepository

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var userInfoRepository: UserInfoRepository

    @Autowired
    protected lateinit var mailTokenRepository: MailTokenRepository

    @Autowired
    @Qualifier("camelCaseObjectMapper")
    protected lateinit var camelCaseObjectMapper: ObjectMapper

    protected fun createUser(
        address: String = ADDRESS.toString(),
    ): User {
        val user = User(address, "email@email.com", null, ZonedDateTime.now(), null)
        return userRepository.save(user)
    }
}
