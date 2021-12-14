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
        "WITH deleted_rows AS (" +
            "    DELETE FROM pending_faucet_address" +
            "    WHERE chain_id = :chainId AND address IN (" +
            "        SELECT address FROM pending_faucet_address" +
            "        WHERE chain_id = :chainId" +
            "        LIMIT :maxAddressesPerTask" +
            "    )" +
            "    RETURNING address" +
            "), selected_rows AS (" +
            "    SELECT * FROM(" +
            "        SELECT :uuid AS uuid, ARRAY(SELECT DISTINCT address FROM deleted_rows) AS addresses," +
            "            :chainId AS chain_id, 'CREATED' AS status, CAST(:timestamp AS TIMESTAMPTZ) AS created_at" +
            "    ) AS potential_task" +
            "    WHERE ARRAY_LENGTH(potential_task.addresses, 1) > 0" +
            ")" +
            "INSERT INTO faucet_task(uuid, addresses, chain_id, status, created_at) " +
            "SELECT * FROM selected_rows",
        nativeQuery = true
    )
    fun flushAddressQueueForChainId(uuid: UUID, chainId: Long, timestamp: ZonedDateTime, maxAddressesPerTask: Int)

    @Query(
        "SELECT DISTINCT chain_id FROM pending_faucet_address",
        nativeQuery = true
    )
    fun fetchChainIdsWithPendingAddresses(): List<Long>

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
}
