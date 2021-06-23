package com.ampnet.identityservice.service.impl

import com.ampnet.core.jwt.JwtTokenUtils
import com.ampnet.identityservice.persistence.repository.RefreshTokenRepository
import com.ampnet.identityservice.persistence.repository.model.RefreshToken
import com.ampnet.identityservice.service.TokenService
import com.ampnet.identityservice.service.pojo.AccessAndRefreshToken
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TokenServiceImpl(private val refreshTokenRepository: RefreshTokenRepository): TokenService {

    @Transactional
    //@Throws(ResourceNotFoundException::class, KeyException::class, TokenException::class)
    override fun generateAccessAndRefreshForUser(userAddress: String): AccessAndRefreshToken {
        deleteRefreshToken(userAddress)
        val token = getRandomToken()
        val refreshToken = refreshTokenRepository.save(RefreshToken(0, userAddress, token, ZonedDateTime.now()))
        // TODO use the new Jwt implementation
        val accessToken = JwtTokenUtils.encodeToken(
            generateUserPrincipalFromUser(user, coop.needUserVerification),
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

    @Transactional
    override fun deleteRefreshToken(userAddress: String) = refreshTokenRepository.deleteByUserAddress(userAddress)

    private fun getRandomToken(): String = (1..REFRESH_TOKEN_LENGTH)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}