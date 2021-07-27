package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.persistence.model.BlockchainTask
import com.ampnet.identityservice.persistence.model.BlockchainTaskStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

interface BlockchainTaskRepository : JpaRepository<BlockchainTask, UUID> {
    @Query(
        "SELECT * FROM blockchain_task WHERE status='IN_PROCESS' " +
            "ORDER BY created_at ASC LIMIT 1 FOR UPDATE SKIP LOCKED;",
        nativeQuery = true
    )
    fun getInProcess(): BlockchainTask?

    @Query(
        "SELECT * FROM blockchain_task WHERE status='CREATED' " +
            "ORDER BY created_at ASC LIMIT 1 FOR UPDATE SKIP LOCKED;",
        nativeQuery = true
    )
    fun getFirstPending(): BlockchainTask?

    @Modifying
    @Transactional
    @Query("UPDATE BlockchainTask SET status = :status, updatedAt = :time, hash = :hash WHERE uuid = :uuid")
    fun setStatus(
        uuid: UUID,
        status: BlockchainTaskStatus,
        hash: String? = null,
        time: ZonedDateTime = ZonedDateTime.now()
    )
}
