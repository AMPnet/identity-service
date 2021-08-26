package com.ampnet.identityservice.service

import com.ampnet.identityservice.service.impl.PinataResponse

interface PinataService {
    fun getUserJwt(address: String): PinataResponse
}
