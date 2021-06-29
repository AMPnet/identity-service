package com.ampnet.identityservice.service.pojo

data class VeriffSessionResponse(
    val status: String,
    val verification: VeriffSessionVerificationResponse
)

data class VeriffSessionVerificationResponse(
    val id: String,
    val url: String,
    val vendorData: String,
    val host: String,
    val status: String,
    val sessionToken: String
)
