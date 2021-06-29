package com.ampnet.identityservice.service

import com.ampnet.identityservice.persistence.model.User

interface UserService {
    fun find(address: String): User?
}