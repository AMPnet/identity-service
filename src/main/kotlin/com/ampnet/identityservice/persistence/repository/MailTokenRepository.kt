package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.persistence.model.MailToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MailTokenRepository : JpaRepository<MailToken, Int> {
    fun findByToken(token: UUID): MailToken?
    fun findByUserAddressOrderByCreatedAtDesc(address: String): List<MailToken>
}
