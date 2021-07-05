package com.ampnet.identityservice.service.pojo

data class VeriffEvent(
    val id: String,
    val attemptId: String,
    val feature: String,
    val code: Int,
    val action: VeriffEventAction,
    val vendorData: String?
)

@Suppress("EnumNaming")
enum class VeriffEventAction {
    started, submitted
}
