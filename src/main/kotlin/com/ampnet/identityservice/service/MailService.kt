package com.ampnet.identityservice.service

interface MailService {
    fun sendEmailConfirmation(receiver: String)
}
