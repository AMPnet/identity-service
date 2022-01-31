package com.ampnet.identityservice.exception

class InvalidRequestException(val errorCode: ErrorCode, exceptionMessage: String, throwable: Throwable? = null) :
    Exception(exceptionMessage, throwable) {
    companion object {
        private const val serialVersionUID: Long = 808617040421268499L
    }
}

class ResourceNotFoundException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage) {
    companion object {
        private const val serialVersionUID: Long = -3758515515385343290L
    }
}

class InternalException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage) {
    companion object {
        private const val serialVersionUID: Long = -2656013978175834059L
    }
}

class VeriffException(exceptionMessage: String, throwable: Throwable? = null) : Exception(exceptionMessage, throwable) {
    companion object {
        private const val serialVersionUID: Long = -2048763908242049348L
    }
}

class ReCaptchaException(
    exceptionMessage: String,
    throwable: Throwable? = null
) : Exception(exceptionMessage, throwable) {
    companion object {
        private const val serialVersionUID: Long = -3031306135593514621L
    }
}
