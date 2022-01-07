package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.persistence.model.AutoInvestTask
import com.ampnet.identityservice.persistence.model.AutoInvestTaskHistoryStatus
import com.ampnet.identityservice.persistence.model.AutoInvestTaskStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

interface AutoInvestTaskRepository : JpaRepository<AutoInvestTask, UUID> {
    @Query(
        """INSERT INTO auto_invest_task(uuid, chain_id, user_wallet_address, campaign_contract_address, amount,
                                        status, created_at)
           VALUES (:#{#task.uuid}, :#{#task.chainId}, :#{#task.userWalletAddress},
                   :#{#task.campaignContractAddress}, :#{#task.amount}, :#{#task.status.name()}, :#{#task.createdAt})
           ON CONFLICT ON CONSTRAINT per_user_campaign DO UPDATE
           SET (amount, created_at) = (:#{#task.amount}, :#{#task.createdAt})
           WHERE auto_invest_task.status = EXCLUDED.status""",
        nativeQuery = true
    )
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    fun createOrUpdate(task: AutoInvestTask): Int

    @Query(
        """SELECT * FROM auto_invest_task
           WHERE status = :#{#status.name()}
           FOR UPDATE SKIP LOCKED""",
        nativeQuery = true
    )
    fun findByStatus(status: AutoInvestTaskStatus): List<AutoInvestTask>

    @Query("UPDATE AutoInvestTask SET status = :status WHERE uuid IN :ids")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    fun updateStatusForIds(ids: List<UUID>, status: AutoInvestTaskStatus)

    @Query("UPDATE AutoInvestTask SET status = :status, hash = :hash WHERE uuid IN :ids")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    fun updateStatusAndHashForIds(ids: List<UUID>, status: AutoInvestTaskStatus, hash: String)

    fun findByChainIdAndUserWalletAddress(chainId: Long, userWalletAddress: String): List<AutoInvestTask>

    fun findByUserWalletAddressAndCampaignContractAddressAndChainId(
        userWalletAddress: String,
        campaignContractAddress: String,
        chainId: Long
    ): AutoInvestTask?

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(
        """WITH deleted_rows AS (
               DELETE FROM auto_invest_task WHERE uuid IN :uuids
               RETURNING uuid, chain_id, user_wallet_address, campaign_contract_address, amount, hash, created_at
           )
           INSERT INTO auto_invest_task_history(uuid, chain_id, user_wallet_address, campaign_contract_address,
                       amount, status, hash, created_at, completed_at)
           SELECT uuid, chain_id, user_wallet_address, campaign_contract_address, amount, :#{#status.name()}, hash,
                  created_at, :completedAt
           FROM deleted_rows""",
        nativeQuery = true
    )
    fun completeTasks(uuids: List<UUID>, status: AutoInvestTaskHistoryStatus, completedAt: ZonedDateTime)

    @Transactional
    @Query(
        "SELECT uuid\\:\\:VARCHAR FROM auto_invest_task_history WHERE status = :#{#status.name()}",
        nativeQuery = true
    )
    fun getHistoricalUuidsForStatus(status: AutoInvestTaskHistoryStatus): List<UUID>
}
