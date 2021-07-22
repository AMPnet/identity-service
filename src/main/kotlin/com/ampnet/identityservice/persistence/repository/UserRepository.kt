package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.persistence.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByAddress(address: String): User?
    fun findByUserInfoUuid(userInfoUuid: UUID): User?
}
