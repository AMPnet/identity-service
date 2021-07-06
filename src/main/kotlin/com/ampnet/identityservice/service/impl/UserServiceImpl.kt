package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.controller.pojo.request.KycTestRequest
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.ResourceNotFoundException
import com.ampnet.identityservice.persistence.model.Document
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.persistence.model.UserInfo
import com.ampnet.identityservice.persistence.repository.UserInfoRepository
import com.ampnet.identityservice.persistence.repository.UserRepository
import com.ampnet.identityservice.service.UserService
import com.ampnet.identityservice.service.pojo.UserWithInfo
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val userInfoRepository: UserInfoRepository
) : UserService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getUser(address: String): User = userRepository.findByAddress(address)
        ?: throw ResourceNotFoundException(ErrorCode.USER_JWT_MISSING, "Missing user with address: $address")

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
    override fun updateEmail(email: String, address: String): User = getUser(address).apply { this.email = email }

    @Transactional
    override fun createUser(address: String): User =
        userRepository.findByAddress(address) ?: kotlin.run {
            logger.info { "User is created for address: $address" }
            userRepository.save(User(address))
        }

    @Transactional
    override fun verifyUserWithTestData(request: KycTestRequest): UserWithInfo {
        val user = getUser(request.address)
        val userInfo = UserInfo(
            UUID.randomUUID(), "44927492-8799-406e-8076-933bc9164ebc",
            request.firstName, request.lastName, null, null,
            Document("DRIVERS_LICENSE", "GB", "MORGA753116SM9IJ", "2022-04-20", null),
            null, null, ZonedDateTime.now(), true, false
        )
        userInfoRepository.save(userInfo)
        user.userInfoUuid = userInfo.uuid
        return UserWithInfo(user, userInfo)
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
}
