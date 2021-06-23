package com.ampnet.identityservice.controller.pojo

data class PayloadRequest(val address: String)

data class AuthorizationRequest(val address: String, val signedPayload: String)
