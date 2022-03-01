package com.ampnet.identityservice.controller.pojo.request

data class PayloadRequest(val address: String)

data class AuthorizationRequest(val address: String, val signedPayload: String, val chainId: Long?)
