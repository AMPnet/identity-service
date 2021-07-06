package com.ampnet.identityservice.persistence.model

import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "app_user")
class User(
    @Id
    val address: String,

    @Column
    var email: String?,

    @Column
    var userInfoUuid: UUID?,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column
    var language: String?
) {
    constructor(address: String) : this (address, null, null, ZonedDateTime.now(), null)
}
