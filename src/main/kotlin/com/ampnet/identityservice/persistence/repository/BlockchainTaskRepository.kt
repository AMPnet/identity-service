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
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(
        "INSERT INTO pending_blockchain_address(address, chain_id, payload) VALUES (:address, :chainId, :payload)",
        nativeQuery = true
    )
    fun addAddressToQueue(address: String, chainId: Long, payload: String? = null)

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(
        """WITH deleted_rows AS (
               DELETE FROM pending_blockchain_address
               WHERE chain_id = :chainId 
               AND (:payload IS NULL OR payload = CAST(:payload AS TEXT)) 
               AND address IN (
                   SELECT address FROM pending_blockchain_address
                   WHERE chain_id = :chainId
                   LIMIT :maxAddressesPerTask
               )
               RETURNING address
           ), selected_rows AS (
               SELECT * FROM(
                   SELECT :uuid AS uuid, ARRAY(SELECT DISTINCT address FROM deleted_rows) AS addresses,
                       :chainId AS chain_id, 'CREATED' AS status, CAST(:timestamp AS TIMESTAMPTZ) AS created_at,
                       CAST(:payload AS TEXT) AS payload
               ) AS potential_task
               WHERE ARRAY_LENGTH(potential_task.addresses, 1) > 0
           )
           INSERT INTO blockchain_task(uuid, addresses, chain_id, status, created_at, payload)
           SELECT * FROM selected_rows""",
        nativeQuery = true
    )
    fun flushAddressQueueForChainId(
        uuid: UUID,
        chainId: Long,
        timestamp: ZonedDateTime,
        maxAddressesPerTask: Int,
        payload: String? = null
    )

    @Query(
        "SELECT DISTINCT chain_id FROM pending_blockchain_address",
        nativeQuery = true
    )
    fun fetchChainIdsWithPendingAddresses(): List<Long>

    @Query(
        "SELECT DISTINCT payload FROM pending_blockchain_address",
        nativeQuery = true
    )
    fun fetchPayloadsWithPendingAddresses(): List<String>

    @Query(
        """SELECT * FROM blockchain_task WHERE status='IN_PROCESS'
           LIMIT 1 FOR UPDATE SKIP LOCKED""",
        nativeQuery = true
    )
    fun getInProcess(): BlockchainTask?

    @Query(
        """SELECT * FROM blockchain_task WHERE status='CREATED'
           LIMIT 1 FOR UPDATE SKIP LOCKED""",
        nativeQuery = true
    )
    fun getPending(): BlockchainTask?

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(
        """UPDATE BlockchainTask SET status = :status, updatedAt = :time, payload = :payload, hash = :hash 
           WHERE uuid = :uuid"""
    )
    fun setStatus(
        uuid: UUID,
        status: BlockchainTaskStatus,
        hash: String? = null,
        payload: String? = null,
        time: ZonedDateTime = ZonedDateTime.now()
    )
}
