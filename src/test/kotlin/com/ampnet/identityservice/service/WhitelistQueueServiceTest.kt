package com.ampnet.identityservice.service

import com.ampnet.identityservice.ManualFixedScheduler
import com.ampnet.identityservice.persistence.model.BlockchainTask
import com.ampnet.identityservice.persistence.model.BlockchainTaskStatus
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime

class WhitelistQueueServiceTest : BlockchainQueueTestBase() {

    @Autowired
    private lateinit var whitelistQueueScheduler: ManualFixedScheduler

    override var queueScheduler: ManualFixedScheduler
        get() = whitelistQueueScheduler
        set(_) {}

    override val payload: String
        get() = "0x75c7193e8e0c8179a0cb2ff190bc8e4681ffa8c7"

    override fun createTask(
        status: BlockchainTaskStatus,
        addresses: List<String>,
        chain: Long,
        hash: String?,
        updatedAt: ZonedDateTime?
    ): BlockchainTask {
        val task = BlockchainTask(
            uuidProvider.getUuid(),
            addresses.toTypedArray(),
            chain,
            status,
            this.payload,
            hash,
            zonedDateTimeProvider.getZonedDateTime(),
            updatedAt
        )
        return blockchainTaskRepository.save(task)
    }

    override fun addAddressToQueue(address: String, chainId: Long) {
        blockchainTaskRepository.addAddressToQueue(address, chainId, this.payload)
    }

    override fun mockBlockchainTaskSuccessfulResponse() {
        given(blockchainService.whitelistAddresses(any(), any(), any()))
            .willReturn(hash)
    }

    override fun mockBlockchainTaskExceptionResponse() {
        given(blockchainService.whitelistAddresses(any(), any(), any()))
            .willThrow(RuntimeException())
    }

    override fun mockBlockchainHashNullResponse() {
        given(blockchainService.whitelistAddresses(any(), any(), any()))
            .willReturn(null)
    }
}