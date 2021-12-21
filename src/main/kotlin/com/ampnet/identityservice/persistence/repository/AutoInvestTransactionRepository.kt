package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.persistence.model.AutoInvestTransaction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AutoInvestTransactionRepository : JpaRepository<AutoInvestTransaction, UUID> {
    fun findByChainIdAndHash(chainId: Long, hash: String): AutoInvestTransaction?
}
