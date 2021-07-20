package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.persistence.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, String> {
    fun findByAddress(address: String): User?
}
