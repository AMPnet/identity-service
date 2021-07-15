package com.ampnet.identityservice.persistence.model

import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "mail_token")
class MailToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    val userAddress: String,

    @Column(nullable = false)
    val email: String,

    @Column(nullable = false)
    val token: UUID,

    @Column(nullable = false)
    val createdAt: ZonedDateTime
) {
    fun isExpired(currentTime: ZonedDateTime): Boolean {
        return createdAt.plusDays(1).isBefore(currentTime)
    }
}
