package com.ampnet.identityservice.service

import com.ampnet.identityservice.service.pojo.AccessAndRefreshToken
import com.ampnet.identityservice.util.WalletAddress

interface TokenService {
    fun generateAccessAndRefreshForUser(address: WalletAddress): AccessAndRefreshToken
    fun generateAccessAndRefreshFromRefreshToken(token: String): AccessAndRefreshToken
    fun deleteRefreshToken(address: WalletAddress)
}
