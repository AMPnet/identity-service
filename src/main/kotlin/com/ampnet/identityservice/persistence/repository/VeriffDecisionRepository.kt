package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.persistence.model.VeriffDecision
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface VeriffDecisionRepository : JpaRepository<VeriffDecision, String> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM VeriffDecision decision WHERE decision.id = :id")
    fun deleteByIdIfPresent(id: String)
}
