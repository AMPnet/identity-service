package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.controller.pojo.request.KycTestRequest
import com.ampnet.identityservice.controller.pojo.request.WhitelistRequest
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException
import com.ampnet.identityservice.exception.ResourceNotFoundException
import com.ampnet.identityservice.persistence.model.Document
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.persistence.model.UserInfo
import com.ampnet.identityservice.persistence.repository.UserInfoRepository
import com.ampnet.identityservice.persistence.repository.UserRepository
import com.ampnet.identityservice.service.UserService
import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.WhitelistQueueService
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import com.ampnet.identityservice.service.pojo.UserResponse
import com.ampnet.identityservice.service.unwrap
import com.ampnet.identityservice.util.WalletAddress
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
@Suppress("TooManyFunctions")
class UserServiceImpl(
    private val uuidProvider: UuidProvider,
    private val zonedDateTimeProvider: ZonedDateTimeProvider,
    private val userRepository: UserRepository,
    private val userInfoRepository: UserInfoRepository,
    private val whitelistQueueService: WhitelistQueueService
) : UserService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getUserResponse(address: WalletAddress): UserResponse = UserResponse(getUser(address))

    @Transactional
    @Throws(ResourceNotFoundException::class)
    override fun connectUserInfo(userAddress: WalletAddress, sessionId: String): UserResponse {
        val userInfo = userInfoRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).firstOrNull()
            ?: throw ResourceNotFoundException(ErrorCode.REG_INCOMPLETE, "Missing UserInfo with session id: $sessionId")
        val user = getUser(userAddress)
        verifyUser(user, userInfo)
        logger.info { "Connected UserInfo: ${userInfo.uuid} to user: $userAddress" }
        return UserResponse(user)
    }

    @Transactional
    override fun createUser(address: WalletAddress): UserResponse {
        val user = userRepository.findByAddress(address.value) ?: kotlin.run {
            logger.info { "User is created for address: $address" }
            userRepository.save(User(address.value))
        }
        return UserResponse(user)
    }

    @Transactional
    override fun updateEmail(email: String, address: WalletAddress): UserResponse {
        logger.debug { "Updating mail for address: $address with new email: $email" }
        val user = getUser(address)
        user.email = email.lowercase()
        return UserResponse(user)
    }

    @Suppress("MagicNumber")
    @Transactional
    override fun verifyUserWithTestData(request: KycTestRequest): UserResponse {
        val user = getUser(WalletAddress(request.address))
        val now = zonedDateTimeProvider.getZonedDateTime().withZoneSameInstant(ZoneId.of("UTC"))
        val validUntil = now.plusDays(10).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val userInfo = UserInfo(
            uuidProvider.getUuid(), "44927492-8799-406e-8076-933bc9164ebc",
            request.firstName, request.lastName, null, null,
            Document("DRIVERS_LICENSE", "GB", "MORGA753116SM9IJ", validUntil, null),
            null, null, zonedDateTimeProvider.getZonedDateTime(), true, false
        )
        userInfoRepository.save(userInfo)
        verifyUser(user, userInfo)
        return UserResponse(user)
    }

    @Throws(InvalidRequestException::class)
    override fun whitelistAddress(userAddress: WalletAddress, request: WhitelistRequest) {
        val user = getUser(userAddress)
        user.userInfoUuid?.let { userInfoUuid ->
            if (isDocumentExpired(userInfoUuid)) {
                disconnectUserInfo(user)
                throw InvalidRequestException(ErrorCode.REG_VERIFF, "Expired KYC data for user: $userAddress")
            }
            whitelistQueueService.addAddressToQueue(userAddress, request)
        } ?: throw InvalidRequestException(ErrorCode.REG_VERIFF, "Missing KYC data for user: $userAddress")
    }

    private fun verifyUser(user: User, userInfo: UserInfo): User {
        disconnectUserInfo(user)
        userInfo.connected = true
        user.userInfoUuid = userInfo.uuid
        return user
    }

    private fun getUser(address: WalletAddress): User = userRepository.findByAddress(address.value)
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

    private fun isDocumentExpired(userInfoUuid: UUID): Boolean =
        userInfoRepository.findById(userInfoUuid).unwrap()?.document?.validUntil?.let { validUntil ->
            val expiryDate = LocalDate.parse(validUntil)
            expiryDate.isBefore(zonedDateTimeProvider.getZonedDateTime().toLocalDate())
        } ?: false
}
