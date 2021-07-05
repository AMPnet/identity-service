package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.persistence.model.VeriffSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VeriffSessionRepository : JpaRepository<VeriffSession, String> {
    fun findByUserAddressOrderByCreatedAtDesc(userAddress: String): List<VeriffSession>
}
