package com.ampnet.identityservice.service.pojo

import com.ampnet.identityservice.persistence.model.VeriffDecision
import com.ampnet.identityservice.persistence.model.VeriffSessionState
import java.util.Locale

data class ServiceVerificationResponse(
    val verificationUrl: String?,
    val state: String,
    val decision: ServiceVerificationDecision?
) {
    constructor(url: String?, state: VeriffSessionState, decision: VeriffDecision? = null) : this(
        url, state.name.lowercase(Locale.getDefault()), decision?.let { ServiceVerificationDecision(it) }
    )
}

data class ServiceVerificationDecision(
    val sessionId: String,
    val status: VeriffStatus,
    val code: Int,
    val reason: String?,
    val reasonCode: Int?,
    val decisionTime: String?,
    val acceptanceTime: String?
) {
    constructor(decision: VeriffDecision) : this(
        decision.id,
        decision.status,
        decision.code,
        decision.reason,
        decision.reasonCode,
        decision.decisionTime,
        decision.acceptanceTime
    )
}
