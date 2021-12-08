package com.ampnet.identityservice.controller

import com.ampnet.identityservice.blockchain.properties.Chain
import com.ampnet.identityservice.persistence.model.FaucetTaskStatus
import com.ampnet.identityservice.persistence.repository.FaucetTaskRepository
import com.ampnet.identityservice.security.WithMockCrowdfundUser
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class FaucetControllerTest : ControllerTestBase() {

    private val defaultChainId = Chain.MATIC_TESTNET_MUMBAI.id
    private val faucetPath = "/faucet/$defaultChainId"

    @Autowired
    private lateinit var faucetTaskRepository: FaucetTaskRepository

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllQueuedFaucetAddresses()
        databaseCleanerService.deleteAllFaucetTasks()
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToRequestFaucetFunds() {
        suppose("User requests faucet funds") {
            mockMvc.perform(
                post(faucetPath)
            )
                .andExpect(status().isOk)
        }

        val taskUuid = UUID.randomUUID()
        val now = ZonedDateTime.now()

        suppose("Address queue is flushed for matic testnet") {
            faucetTaskRepository.flushAddressQueueForChainId(taskUuid, defaultChainId, now)
        }

        verify("Task is created for flushed addresses for matic testnet") {
            val task = faucetTaskRepository.findById(taskUuid)
            Assertions.assertThat(task).hasValueSatisfying {
                Assertions.assertThat(it.addresses).containsExactly("0xef678007d18427e6022059dbc264f27507cd1ffc")
                Assertions.assertThat(it.chainId).isEqualTo(defaultChainId)
                Assertions.assertThat(it.status).isEqualTo(FaucetTaskStatus.CREATED)
            }
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustReturnErrorWhenFaucetIsNotSupportedForChainId() {
        verify("Faucet request fails for unsupported chain") {
            mockMvc.perform(
                post("/faucet/${Chain.ETHEREUM_MAIN.id}")
            )
                .andExpect(status().isBadRequest)
        }
    }
}
