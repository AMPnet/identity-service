package com.ampnet.identityservice.persistence.model

import com.ampnet.identityservice.service.pojo.VeriffStatus
import com.ampnet.identityservice.service.pojo.VeriffVerification
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "veriff_decision")
class VeriffDecision(
    @Id
    val id: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: VeriffStatus,

    @Column(nullable = false)
    val code: Int,

    @Column
    val reason: String?,

    @Column
    val reasonCode: Int?,

    @Column
    val decisionTime: String?,

    @Column
    val acceptanceTime: String?,

    @Column(nullable = false)
    val createdAt: ZonedDateTime
) {
    constructor(verification: VeriffVerification) : this(
        verification.id,
        verification.status,
        verification.code,
        verification.reason,
        verification.reasonCode,
        verification.decisionTime,
        verification.acceptanceTime,
        ZonedDateTime.now()
    )
}
