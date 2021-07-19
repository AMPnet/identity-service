package com.ampnet.identityservice.service

import java.util.UUID

interface MailService {
    fun sendEmailConfirmation(receiver: String, token: UUID)
}
