package com.ampnet.identityservice.exception

@Suppress("MagicNumber")
enum class VeriffVerificationCode(val code: Int) {
    POSITIVE(9001), NEGATIVE(9102), RESUBMISSION(9103), NEGATIVE_EXPIRED(9104);

    companion object {
        private val map = values().associateBy(VeriffVerificationCode::code)
        fun fromInt(type: Int) = map[type]
    }
}

@Suppress("MagicNumber")
enum class VeriffReasonCode(val code: Int) {
    DOC_NOT_USED(101), DOC_TAMPERING(102), PERSON_NOT_MATCH(103),
    NAME_NOT_MATCH(104), SUSPICIOUS_BEH(105), KNOWN_FRAUD(106),
    ABUSE(107), ABUSE_DUPLICATED_USER(108), ABUSE_DUPLICATED_DEVICE(109), ABUSE_DUPLICATED_ID(110),
    PHOTO_MISSING(201), FACE_NOT_VISIBLE(202), DOC_NOT_VISIBLE(203), POOR_IMAGE(204),
    DOC_DAMAGED(205), DOC_TYPE_NOT_SUPPORTED(206), DOC_EXPIRED(207);

    companion object {
        private val map = values().associateBy(VeriffReasonCode::code)
        fun fromInt(type: Int?) = map[type]
    }
}
