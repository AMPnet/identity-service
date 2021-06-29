package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.persistence.repository.UserRepository
import com.ampnet.identityservice.service.UserService
import org.springframework.stereotype.Service

@Service
class UserServiceImpl(private val userRepository: UserRepository): UserService {

    override fun find(address: String): User? = userRepository.findByAddress(address)
}