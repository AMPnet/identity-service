package com.ampnet.identityservice.controller

import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.blockchain.BlockchainService
import com.ampnet.identityservice.blockchain.IIssuer
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.config.DatabaseCleanerService
import com.ampnet.identityservice.controller.pojo.request.KycTestRequest
import com.ampnet.identityservice.controller.pojo.request.WhitelistRequest
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.persistence.repository.BlockchainTaskRepository
import com.ampnet.identityservice.persistence.repository.UserRepository
import com.ampnet.identityservice.security.WithMockCrowdfundUser
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.StaticGasProvider
import java.lang.Thread.sleep
import java.math.BigInteger

@SpringBootTest
@ExtendWith(value = [SpringExtension::class])
@ActiveProfiles("secret")
@Disabled("Not for automated testing. Error processing transaction request: insufficient funds for gas * price + value")
class Web3jTest : TestBase() {

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    @Autowired
    private lateinit var databaseCleanerService: DatabaseCleanerService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var blockchainTaskRepository: BlockchainTaskRepository

    @Autowired
    private lateinit var zonedDateTimeProvider: ZonedDateTimeProvider

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var blockchainService: BlockchainService

    private val web3j by lazy { Web3j.build(HttpService(applicationProperties.provider.blockchainApi)) }

    private val address = "0x9a72aD187229e9338c7f21E019544947Fb25d473"
    private val issuerAddress = "0xD17574450885C1b898bc835Ff9CB5b44A3601c24"

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun init(wac: WebApplicationContext) {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()

        databaseCleanerService.deleteAllBlockchainTasks()
    }

    @AfterEach
    fun after() {
        databaseCleanerService.deleteAllBlockchainTasks()
    }

    @Test
    fun mustReturnTrueForWalletApproved() {
        val credentials = Credentials.create(applicationProperties.smartContract.privateKey)
        val contract = IIssuer.load(
            "0x8Ba5082E853b87E8D92970506EBb7c7097d6F640", web3j, credentials,
            StaticGasProvider(BigInteger("22000000000"), BigInteger("510000"))
        )
        contract.approveWallet(applicationProperties.smartContract.walletAddress).send()
        val isWalletApproved = contract.isWalletApproved(applicationProperties.smartContract.walletAddress).send()
        assertThat(isWalletApproved).isTrue
    }

    @Test
    @WithMockCrowdfundUser(address = "0x9a72aD187229e9338c7f21E019544947Fb25d473")
    fun mustWhitelistAddress() {
        suppose("There is a user") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllUserInfos()

            val user = User(address, "email@mail.co", null, zonedDateTimeProvider.getZonedDateTime(), null)
            userRepository.save(user)
        }
        suppose("User completed KYC") {
            val request = objectMapper.writeValueAsString(KycTestRequest(address, "first", "last"))
            mockMvc.perform(
                MockMvcRequestBuilders.post("/test/kyc")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }

        verify("User can whitelist address for issuer") {
            val request = objectMapper.writeValueAsString(WhitelistRequest(issuerAddress))
            mockMvc.perform(
                MockMvcRequestBuilders.post("/user/whitelist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }
        verify("Wait for task to complete") {
            sleep(1000)
            waitUntilTasksAreProcessed(10)
            val isWhitelisted = blockchainService.isWhitelisted(address, issuerAddress)
            assertThat(isWhitelisted).isTrue()
        }
    }

    private fun waitUntilTasksAreProcessed(retry: Int = 5) {
        if (retry == 0) return
        sleep(applicationProperties.queue.initialDelay * 2)
        blockchainTaskRepository.getPending()?.let {
            sleep(applicationProperties.queue.polling * 2)
            waitUntilTasksAreProcessed(retry - 1)
        }
        blockchainTaskRepository.getInProcess()?.let {
            sleep(applicationProperties.queue.polling * 2)
            waitUntilTasksAreProcessed(retry - 1)
        }
    }
}
