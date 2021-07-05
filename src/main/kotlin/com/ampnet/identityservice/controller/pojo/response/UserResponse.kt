package com.ampnet.identityservice.controller.pojo.response

import com.ampnet.identityservice.persistence.model.User

data class UserResponse(
    val address: String,
    val email: String?
) {
    constructor(user: User) : this(
        user.address,
        user.email
    )
}
