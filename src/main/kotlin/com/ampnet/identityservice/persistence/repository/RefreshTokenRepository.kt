package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.persistence.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Int> {
    fun deleteByUserAddress(userAddress: String)
    fun findByToken(token: String): RefreshToken?
}
