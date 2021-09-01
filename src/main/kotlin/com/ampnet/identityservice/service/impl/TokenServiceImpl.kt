package com.ampnet.identityservice.service.impl

import com.ampnet.core.jwt.JwtTokenUtils
import com.ampnet.core.jwt.exception.KeyException
import com.ampnet.core.jwt.exception.TokenException
import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InvalidRequestException
import com.ampnet.identityservice.persistence.model.RefreshToken
import com.ampnet.identityservice.persistence.repository.RefreshTokenRepository
import com.ampnet.identityservice.service.TokenService
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import com.ampnet.identityservice.service.pojo.AccessAndRefreshToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
class TokenServiceImpl(
    private val zonedDateTimeProvider: ZonedDateTimeProvider,
    private val applicationProperties: ApplicationProperties,
    private val refreshTokenRepository: RefreshTokenRepository
) : TokenService {

    private companion object {
        const val REFRESH_TOKEN_LENGTH = 128
    }

    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('-', '_', '+')
    private val secureRandom = SecureRandom()

    @Transactional
    @Throws(KeyException::class, TokenException::class)
    override fun generateAccessAndRefreshForUser(address: String): AccessAndRefreshToken {
        val lowercaseAddress = address.lowercase()
        deleteRefreshToken(lowercaseAddress)
        val token = getRandomToken()
        val refreshToken = refreshTokenRepository.save(
            RefreshToken(0, lowercaseAddress, token, zonedDateTimeProvider.getZonedDateTime())
        )
        val accessToken = JwtTokenUtils.encodeToken(
            lowercaseAddress,
            applicationProperties.jwt.privateKey,
            applicationProperties.jwt.accessTokenValidityInMilliseconds()
        )
        return AccessAndRefreshToken(
            accessToken,
            applicationProperties.jwt.accessTokenValidityInMilliseconds(),
            refreshToken.token,
            applicationProperties.jwt.refreshTokenValidityInMilliseconds()
        )
    }

    @Suppress("MagicNumber")
    @Throws(InvalidRequestException::class, KeyException::class, TokenException::class)
    override fun generateAccessAndRefreshFromRefreshToken(token: String): AccessAndRefreshToken {
        val refreshToken = refreshTokenRepository.findByToken(token)
            ?: throw InvalidRequestException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN, "Non existing refresh token")
        val expiration = refreshToken.createdAt
            .plusMinutes(applicationProperties.jwt.refreshTokenValidityInMinutes)
        val refreshTokenExpiresIn: Long =
            expiration.toEpochSecond() - zonedDateTimeProvider.getZonedDateTime().toEpochSecond()
        if (refreshTokenExpiresIn <= 0) {
            refreshTokenRepository.delete(refreshToken)
            throw InvalidRequestException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN, "Refresh token expired")
        }
        val accessToken = JwtTokenUtils.encodeToken(
            refreshToken.userAddress,
            applicationProperties.jwt.privateKey,
            applicationProperties.jwt.accessTokenValidityInMilliseconds()
        )
        return AccessAndRefreshToken(
            accessToken,
            applicationProperties.jwt.accessTokenValidityInMilliseconds(),
            refreshToken.token,
            refreshTokenExpiresIn * 1000
        )
    }

    @Transactional
    override fun deleteRefreshToken(address: String) = refreshTokenRepository.deleteByUserAddress(address.lowercase())

    private fun getRandomToken(): String = (1..REFRESH_TOKEN_LENGTH)
        .map { secureRandom.nextInt(charPool.size) }
        .map(charPool::get)
        .joinToString("")
}
