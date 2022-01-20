package com.ampnet.identityservice.service.pojo

import com.ampnet.identityservice.persistence.model.User

data class UserResponse(
    val address: String,
    val email: String?,
    val emailVerified: Boolean,
    val kycCompleted: Boolean
) {
    constructor(user: User) : this(user.address, user.email, true, user.userInfoUuid != null)
}
