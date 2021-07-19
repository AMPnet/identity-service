package com.ampnet.identityservice.service.pojo

data class UserResponse(
    val address: String,
    val email: String?,
    val emailVerified: Boolean,
    val kycCompleted: Boolean
)
