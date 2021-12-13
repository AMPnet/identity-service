package com.ampnet.identityservice.controller

import com.ampnet.identityservice.blockchain.properties.Chain
import com.ampnet.identityservice.persistence.model.FaucetTaskStatus
import com.ampnet.identityservice.persistence.repository.FaucetTaskRepository
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
    private val address = "0xef678007d18427e6022059dbc264f27507cd1ffc"
    private val faucetPath = "/faucet/$defaultChainId/$address"

    @Autowired
    private lateinit var faucetTaskRepository: FaucetTaskRepository

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllQueuedFaucetAddresses()
        databaseCleanerService.deleteAllFaucetTasks()
    }

    @Test
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
                Assertions.assertThat(it.addresses).containsExactly(address)
                Assertions.assertThat(it.chainId).isEqualTo(defaultChainId)
                Assertions.assertThat(it.status).isEqualTo(FaucetTaskStatus.CREATED)
            }
        }
    }

    @Test
    fun mustReturnErrorWhenFaucetIsNotSupportedForChainId() {
        verify("Faucet request fails for unsupported chain") {
            mockMvc.perform(
                post("/faucet/0/$address")
            )
                .andExpect(status().isBadRequest)
        }
    }
}
