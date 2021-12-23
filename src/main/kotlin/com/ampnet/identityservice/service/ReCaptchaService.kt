package com.ampnet.identityservice.service

interface ReCaptchaService {
    fun validateResponseToken(reCaptchaToken: String?)
}
