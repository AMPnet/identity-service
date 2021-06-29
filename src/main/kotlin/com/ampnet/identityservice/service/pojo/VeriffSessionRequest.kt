package com.ampnet.identityservice.service.pojo

import com.ampnet.identityservice.persistence.model.User
import java.time.ZonedDateTime

data class VeriffSessionRequest(
    val verification: VeriffSessionVerificationRequest
) {
    constructor(user: User, callback: String) : this(VeriffSessionVerificationRequest(user, callback))
}

data class VeriffSessionVerificationRequest(
    val callback: String,
    val person: VeriffSessionPerson,
    val vendorData: String,
    val timestamp: ZonedDateTime
) {
    constructor(user: User, callback: String) : this(
        callback,
        VeriffSessionPerson(null, null),
        user.address,
        ZonedDateTime.now()
    )
}
// TODO check if firstName and lastName is actually required for anything
data class VeriffSessionPerson(
    val firstName: String?,
    val lastName: String?
)
