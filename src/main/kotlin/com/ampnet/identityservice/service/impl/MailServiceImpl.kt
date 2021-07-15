package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.service.MailService
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.net.URL
import java.util.Date

// This class is a temporary solution for sending email.
// For further extensions keep in mind that it will be extracted to new notification-service repo
@Service
class MailServiceImpl(
    private val mailSender: JavaMailSender,
    private val applicationProperties: ApplicationProperties
) : MailService {

    private val baseUrl = URL(applicationProperties.mail.baseUrl).toString()
    private val confirmationSubject = "Confirm your email"
    private val confirmationText = """<h2>Please confirmation your email</h2>
        |
        |<p>Follow the link the confirm your email: <a href="{{& link}}">{{& link}}</a></p>""".trimMargin()

    override fun sendEmailConfirmation(receiver: String) {
        if (applicationProperties.mail.enabled.not()) return
        val confirmationLink = "$baseUrl/user/email"
        val mail = mailSender.createMimeMessage().apply {
            val helper = MimeMessageHelper(this, "UTF-8")
            helper.isValidateAddresses = true
            helper.setFrom(applicationProperties.mail.sender)
            helper.setTo(receiver)
            helper.setSubject(confirmationSubject)
            helper.setText(confirmationText.replace("{{& link}}", confirmationLink), true)
            helper.setSentDate(Date())
        }
        mailSender.send(mail)
    }
}
