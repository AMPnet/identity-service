package com.ampnet.identityservice.exception

enum class ErrorCode(val categoryCode: String, var specificCode: String, var message: String) {
    // Registration: 01
    REG_INCOMPLETE("01", "01", "Invalid signup data"),
    REG_EMAIL_EXPIRED_TOKEN("01", "05", "Failed Email confirmation, token expired"),
    REG_VERIFF("01", "11", "Missing Veriff session"),

    // Authentication: 02
    AUTH_INVALID_REFRESH_TOKEN("02", "08", "Invalid refresh token"),
    AUTH_PAYLOAD_MISSING("02", "09", "Payload missing for address"),
    AUTH_SIGNED_PAYLOAD_INVALID("02", "10", "Signature is invalid"),

    // Users: 03
    USER_JWT_MISSING("03", "01", "Missing user address defined in JWT"),
    USER_PINATA_JWT("03", "02", "Failed to get Pinata JWT for user"),

    // Blockchain: 04
    BLOCKCHAIN_ID("04", "01", "Blockchain id not supported"),
    BLOCKCHAIN_CONFIG_MISSING("04", "02", "Blockchain data is not provided"),
}
