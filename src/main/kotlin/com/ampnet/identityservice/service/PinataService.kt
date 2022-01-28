package com.ampnet.identityservice.service

import com.ampnet.identityservice.service.impl.PinataResponse
import com.ampnet.identityservice.util.WalletAddress

interface PinataService {
    fun getUserJwt(address: WalletAddress): PinataResponse
}
