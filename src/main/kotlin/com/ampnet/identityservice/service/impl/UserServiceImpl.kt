package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.controller.pojo.request.KycTestRequest
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException
import com.ampnet.identityservice.exception.ResourceNotFoundException
import com.ampnet.identityservice.persistence.model.Document
import com.ampnet.identityservice.persistence.model.MailToken
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.persistence.model.UserInfo
import com.ampnet.identityservice.persistence.repository.MailTokenRepository
import com.ampnet.identityservice.persistence.repository.UserInfoRepository
import com.ampnet.identityservice.persistence.repository.UserRepository
import com.ampnet.identityservice.service.BlockchainQueueService
import com.ampnet.identityservice.service.MailService
import com.ampnet.identityservice.service.UserService
import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import com.ampnet.identityservice.service.pojo.UserResponse
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserServiceImpl(
    private val uuidProvider: UuidProvider,
    private val zonedDateTimeProvider: ZonedDateTimeProvider,
    private val userRepository: UserRepository,
    private val userInfoRepository: UserInfoRepository,
    private val mailTokenRepository: MailTokenRepository,
    private val mailService: MailService,
    private val applicationProperties: ApplicationProperties,
    private val blockchainQueueService: BlockchainQueueService
) : UserService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getUserResponse(address: String): UserResponse = generateUserResponse(getUser(address))

    @Transactional
    @Throws(ResourceNotFoundException::class)
    override fun connectUserInfo(userAddress: String, sessionId: String): UserResponse {
        val userInfo = userInfoRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).firstOrNull()
            ?: throw ResourceNotFoundException(ErrorCode.REG_INCOMPLETE, "Missing UserInfo with session id: $sessionId")
        val user = getUser(userAddress)
        verifyUser(user, userInfo)
        logger.info { "Connected UserInfo: ${userInfo.uuid} to user: $userAddress" }
        return generateUserResponse(user)
    }

    @Transactional
    override fun createUser(address: String): UserResponse {
        val user = userRepository.findByAddress(address) ?: kotlin.run {
            logger.info { "User is created for address: $address" }
            userRepository.save(User(address))
        }
        return generateUserResponse(user)
    }

    @Transactional
    override fun updateEmail(email: String, address: String): UserResponse {
        val user = getUser(address)
        logger.info { "Mail confirmation is ${if (applicationProperties.mail.enabled) "enabled" else "disabled"}" }
        if (applicationProperties.mail.enabled.not()) {
            user.email = email
            return generateUserResponse(user)
        }
        val mailToken = MailToken(
            0, address, email, uuidProvider.getUuid(), zonedDateTimeProvider.getZonedDateTime()
        )
        mailTokenRepository.save(mailToken)
        mailService.sendEmailConfirmation(email, mailToken.token)
        user.email = null
        return generateUserResponse(user)
    }

    @Transactional
    override fun confirmMail(token: UUID): UserResponse? {
        mailTokenRepository.findByToken(token)?.let { mailToken ->
            if (mailToken.isExpired(zonedDateTimeProvider.getZonedDateTime())) {
                throw InvalidRequestException(
                    ErrorCode.REG_EMAIL_EXPIRED_TOKEN,
                    "User is trying to confirm mail with expired token: $token"
                )
            }
            mailTokenRepository.delete(mailToken)
            userRepository.findByAddress(mailToken.userAddress)?.let { user ->
                user.email = mailToken.email
                return generateUserResponse(user)
            }
        }
        return null
    }

    @Transactional
    override fun verifyUserWithTestData(request: KycTestRequest): UserResponse {
        val user = getUser(request.address)
        val userInfo = UserInfo(
            uuidProvider.getUuid(), "44927492-8799-406e-8076-933bc9164ebc",
            request.firstName, request.lastName, null, null,
            Document("DRIVERS_LICENSE", "GB", "MORGA753116SM9IJ", "2022-04-20", null),
            null, null, zonedDateTimeProvider.getZonedDateTime(), true, false
        )
        userInfoRepository.save(userInfo)
        verifyUser(user, userInfo)
        return generateUserResponse(user)
    }

    private fun verifyUser(user: User, userInfo: UserInfo): User {
        disconnectUserInfo(user)
        userInfo.connected = true
        user.userInfoUuid = userInfo.uuid
        blockchainQueueService.createWhitelistAddressTask(user.address)
        return user
    }

    private fun generateUserResponse(user: User): UserResponse {
        val (email, emailVerified) = if (user.email == null) {
            Pair(mailTokenRepository.findByUserAddressOrderByCreatedAtDesc(user.address).firstOrNull()?.email, false)
        } else {
            Pair(user.email, true)
        }
        return UserResponse(user.address, email, emailVerified, user.userInfoUuid != null)
    }

    private fun getUser(address: String): User = userRepository.findByAddress(address)
        ?: throw ResourceNotFoundException(ErrorCode.USER_JWT_MISSING, "Missing user with address: $address")

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
