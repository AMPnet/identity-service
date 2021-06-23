package com.ampnet.identityservice.service

import com.ampnet.identityservice.service.pojo.AccessAndRefreshToken

interface TokenService {
    fun generateAccessAndRefreshForUser(userAddress: String): AccessAndRefreshToken
    fun deleteRefreshToken(userAddress: String)
}