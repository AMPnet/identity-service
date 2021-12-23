package com.ampnet.identityservice.exception

class InvalidRequestException(val errorCode: ErrorCode, exceptionMessage: String, throwable: Throwable? = null) :
    Exception(exceptionMessage, throwable)

class ResourceNotFoundException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage)

class InternalException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage)

class VeriffException(exceptionMessage: String, throwable: Throwable? = null) : Exception(exceptionMessage, throwable)

class ReCaptchaException(
    exceptionMessage: String,
    throwable: Throwable? = null
) : Exception(exceptionMessage, throwable)
