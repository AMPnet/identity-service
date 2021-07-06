package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.persistence.model.UserInfo
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserInfoRepository : JpaRepository<UserInfo, UUID> {
    fun findBySessionIdOrderByCreatedAtDesc(sessionId: String): List<UserInfo>
    fun findByFirstNameAndLastName(firstName: String, lastname: String): UserInfo
}
