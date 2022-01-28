package com.ampnet.identityservice.service

import com.ampnet.identityservice.ManualFixedScheduler
import com.ampnet.identityservice.persistence.model.BlockchainTask
import com.ampnet.identityservice.persistence.model.BlockchainTaskStatus
import com.ampnet.identityservice.util.ChainId
import com.ampnet.identityservice.util.TransactionHash
import com.ampnet.identityservice.util.WalletAddress
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime

class FaucetQueueServiceTest : BlockchainQueueTestBase() {

    @Autowired
    private lateinit var faucetQueueScheduler: ManualFixedScheduler

    override var queueScheduler: ManualFixedScheduler
        get() = faucetQueueScheduler
        set(_) {}

    override val payload: String?
        get() = null

    override fun createTask(
        status: BlockchainTaskStatus,
        addresses: List<WalletAddress>,
        chain: ChainId,
        hash: TransactionHash?,
        updatedAt: ZonedDateTime?
    ): BlockchainTask {
        val task = BlockchainTask(
            uuidProvider.getUuid(),
            addresses.map { it.value }.toTypedArray(),
            chain.value,
            status,
            null,
            hash?.value,
            zonedDateTimeProvider.getZonedDateTime(),
            updatedAt
        )
        return blockchainTaskRepository.save(task)
    }

    override fun addAddressToQueue(address: String, chainId: Long) {
        blockchainTaskRepository.addAddressToQueue(address, chainId)
    }

    override fun mockBlockchainTaskSuccessfulResponse() {
        given(blockchainService.sendFaucetFunds(any(), any())).willReturn(hash)
    }

    override fun mockBlockchainTaskExceptionResponse() {
        given(blockchainService.sendFaucetFunds(any(), any())).willThrow(RuntimeException())
    }

    override fun mockBlockchainHashNullResponse() {
        given(blockchainService.sendFaucetFunds(any(), any())).willReturn(null)
    }
}
