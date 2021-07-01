package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.ResourceNotFoundException
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.persistence.repository.UserInfoRepository
import com.ampnet.identityservice.persistence.repository.UserRepository
import com.ampnet.identityservice.service.UserService
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val userInfoRepository: UserInfoRepository
) : UserService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun find(address: String): User? = userRepository.findByAddress(address)

    @Transactional
    @Throws(ResourceNotFoundException::class)
    override fun connectUserInfo(userAddress: String, sessionId: String): User {
        val userInfo = userInfoRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).firstOrNull()
            ?: throw ResourceNotFoundException(ErrorCode.REG_INCOMPLETE, "Missing UserInfo with session id: $sessionId")
        val user = getUser(userAddress)
        disconnectUserInfo(user)
        userInfo.connected = true
        user.userInfoUuid = userInfo.uuid
        logger.info { "Connected UserInfo: ${userInfo.uuid} to user: $userAddress" }
        return user
    }

    @Transactional
    override fun updateEmail(email: String, address: String) {
        getUser(address).apply { this.email = email }
    }

    private fun disconnectUserInfo(user: User) {
        user.userInfoUuid?.let {
            userInfoRepository.findById(it).ifPresent { userInfo ->
                userInfo.connected = false
                userInfo.deactivated = true
                logger.info { "Disconnected old user info: ${userInfo.uuid} for user: ${user.address}" }
            }
        }
    }

    private fun getUser(address: String): User = find(address)
        ?: throw ResourceNotFoundException(ErrorCode.USER_JWT_MISSING, "Missing user with address: $address")
}