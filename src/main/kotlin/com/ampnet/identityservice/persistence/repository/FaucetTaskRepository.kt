package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.persistence.model.FaucetTask
import com.ampnet.identityservice.persistence.model.FaucetTaskStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

interface FaucetTaskRepository : JpaRepository<FaucetTask, UUID> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(
        "INSERT INTO pending_faucet_address(address, chain_id) VALUES (:address, :chainId)",
        nativeQuery = true
    )
    fun addAddressToQueue(address: String, chainId: Long)

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(
        "WITH deleted_rows AS (DELETE FROM pending_faucet_address WHERE chain_id = :chainId RETURNING address) " +
            "INSERT INTO faucet_task(uuid, addresses, chain_id, status, created_at) " +
            "VALUES (:uuid, ARRAY(SELECT DISTINCT address FROM deleted_rows), :chainId, 'CREATED', :timestamp)",
        nativeQuery = true
    )
    fun flushAddressQueueForChainId(uuid: UUID, chainId: Long, timestamp: ZonedDateTime)

    @Query(
        "SELECT * FROM faucet_task WHERE status='IN_PROCESS' " +
            "LIMIT 1 FOR UPDATE SKIP LOCKED",
        nativeQuery = true
    )
    fun getInProcess(): FaucetTask?

    @Query(
        "SELECT * FROM faucet_task WHERE status='CREATED' " +
            "LIMIT 1 FOR UPDATE SKIP LOCKED",
        nativeQuery = true
    )
    fun getPending(): FaucetTask?

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE FaucetTask SET status = :status, updatedAt = :time, hash = :hash WHERE uuid = :uuid")
    fun setStatus(
        uuid: UUID,
        status: FaucetTaskStatus,
        hash: String? = null,
        time: ZonedDateTime = ZonedDateTime.now()
    )

    @Query
    fun findByChainIdAndStatus(chainId: Long, status: FaucetTaskStatus): List<FaucetTask>
}
