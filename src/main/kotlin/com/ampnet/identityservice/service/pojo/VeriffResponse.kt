package com.ampnet.identityservice.service.pojo

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

data class VeriffDocument(
    val number: String?,
    val type: String?,
    val country: String?,
    val validFrom: String?,
    val validUntil: String?
)

data class VeriffRiskLabel(
    val label: String,
    val category: String
)

@Suppress("EnumNaming")
enum class VeriffStatus {
    approved, resubmission_requested, declined, expired, abandoned, review
}
