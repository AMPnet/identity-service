package com.ampnet.identityservice.service.pojo

import java.time.ZonedDateTime

data class VeriffSessionRequest(
    val verification: VeriffSessionVerificationRequest
) {
    constructor(user: UserResponse, callback: String) : this(VeriffSessionVerificationRequest(user, callback))
}

data class VeriffSessionVerificationRequest(
    val callback: String,
    val person: VeriffSessionPerson,
    val vendorData: String,
    val timestamp: ZonedDateTime
) {
    constructor(user: UserResponse, callback: String) : this(
        callback,
        VeriffSessionPerson("Firstname", "Lastname"),
        user.address,
        ZonedDateTime.now()
    )
}

data class VeriffSessionPerson(
    val firstName: String?,
    val lastName: String?
)
