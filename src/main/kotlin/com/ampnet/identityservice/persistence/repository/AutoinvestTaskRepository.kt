package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.persistence.model.AutoinvestTask
import com.ampnet.identityservice.persistence.model.AutoinvestTaskStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface AutoinvestTaskRepository : JpaRepository<AutoinvestTask, UUID> {
    @Query(
        "INSERT INTO autoinvest_task(uuid, chain_id, user_wallet_address, campaign_contract_address, amount," +
            " status, created_at)" +
            " VALUES (:#{#task.uuid}, :#{#task.chainId}, :#{#task.userWalletAddress}," +
            " :#{#task.campaignContractAddress}, :#{#task.amount}, :#{#task.status.name()}, :#{#task.createdAt})" +
            " ON CONFLICT ON CONSTRAINT per_user_campaign DO UPDATE" +
            " SET (amount, created_at) = (autoinvest_task.amount + :#{#task.amount}, :#{#task.createdAt})",
        nativeQuery = true
    )
    @Modifying
    @Transactional
    fun createOrUpdate(task: AutoinvestTask)

    fun findByChainIdAndStatus(chainId: Long, status: AutoinvestTaskStatus): List<AutoinvestTask>
}
