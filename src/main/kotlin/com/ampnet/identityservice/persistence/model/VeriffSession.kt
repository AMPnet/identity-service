package com.ampnet.identityservice.persistence.model

import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "veriff_session")
class VeriffSession(
    @Id
    val id: String,

    @Column(nullable = false)
    val userAddress: String,

    @Column
    val url: String?,

    @Column
    val vendorData: String?,

    @Column
    val host: String?,

    @Column
    val status: String?,

    @Column(nullable = false)
    var connected: Boolean,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column(nullable = false)
    var state: VeriffSessionState
) {
    constructor(veriffSessionResponse: VeriffSessionResponse, userAddress: String) : this(
        veriffSessionResponse.verification.id,
        userAddress,
        veriffSessionResponse.verification.url,
        veriffSessionResponse.verification.vendorData,
        veriffSessionResponse.verification.host,
        veriffSessionResponse.status,
        false,
        ZonedDateTime.now(),
        VeriffSessionState.CREATED
    )
}

data class VeriffSessionResponse(
    val status: String?,
    val verification: VeriffSessionVerificationResponse
)

data class VeriffSessionVerificationResponse(
    val id: String,
    val url: String?,
    val vendorData: String?,
    val host: String?,
    val status: String?,
    val sessionToken: String
)

enum class VeriffSessionState(val id: Int) {
    CREATED(0), STARTED(1), SUBMITTED(2)
}
