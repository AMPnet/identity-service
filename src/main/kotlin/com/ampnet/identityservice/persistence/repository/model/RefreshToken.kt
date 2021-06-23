package com.ampnet.identityservice.persistence.repository.model

import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "refresh_token")
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var userAddress: String,

    @Column(nullable = false, length = 128)
    var token: String,

    @Column(nullable = false)
    var createdAt: ZonedDateTime
)
