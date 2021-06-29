package com.ampnet.identityservice.persistence.model

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

data class VeriffResponse(
    val status: String,
    val verification: VeriffVerification?,
    val technicalData: VeriffTechnicalData?
)

data class VeriffTechnicalData(
    val ip: String?
)

data class VeriffVerification(
    val id: String,
    val status: VeriffStatus,
    val code: Int,
    val reason: String?,
    val person: VeriffPerson?,
    val document: VeriffDocument?,
    val reasonCode: Int?,
    val decisionTime: String?,
    val acceptanceTime: String?,
    val riskLabels: List<VeriffRiskLabel>?,
    val vendorData: String?
)

data class VeriffPerson(
    val firstName: String?,
    val lastName: String?,
    val idNumber: String?,
    val dateOfBirth: String?,
    val nationality: String?,
    val pepSanctionMatch: String?,
    val gender: String?,
    val yearOfBirth: String?,
    val placeOfBirth: String?
)
data class VeriffRiskLabel(
    val label: String,
    val category: String
)

@Suppress("EnumNaming")
enum class VeriffStatus {
    approved, resubmission_requested, declined, expired, abandoned, review
}
