package com.ampnet.identityservice.exception

class InvalidRequestException(val errorCode: ErrorCode, exceptionMessage: String, throwable: Throwable? = null) :
    Exception(exceptionMessage, throwable)

class ResourceNotFoundException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage)
