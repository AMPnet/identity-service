package com.ampnet.identityservice.service.pojo

import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.persistence.model.UserInfo

data class UserWithInfo(
    val address: String,
    val firstName: String,
    val lastName: String
) {
    constructor(user: User, userInfo: UserInfo) : this(
        user.address,
        userInfo.firstName,
        userInfo.lastName
    )
}
