package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.persistence.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Int> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(
        """DELETE FROM RefreshToken WHERE userAddress = :userAddress"""
    )
    fun deleteByUserAddress(userAddress: String)
    fun findByToken(token: String): RefreshToken?
}
