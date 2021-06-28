package com.ampnet.identityservice.service

import com.ampnet.identityservice.service.pojo.AccessAndRefreshToken

interface TokenService {
    fun generateAccessAndRefreshForUser(address: String): AccessAndRefreshToken
    fun generateAccessAndRefreshFromRefreshToken(token: String): AccessAndRefreshToken
    fun deleteRefreshToken(address: String)
}
