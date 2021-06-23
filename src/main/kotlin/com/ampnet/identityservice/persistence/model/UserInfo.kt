package com.ampnet.identityservice.persistence.model

import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "user_info")
class UserInfo(
    @Id
    val uuid: UUID,

    @Column(nullable = false)
    var sessionId: String,

    @Column(nullable = false)
    var firstName: String,

    @Column(nullable = false)
    var lastName: String,

    @Column
    var idNumber: String?,

    @Column
    var dateOfBirth: String?,

    @Embedded
    var document: Document,

    @Column
    var nationality: String?,

    @Column
    var placeOfBirth: String?,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = false)
    var connected: Boolean,

    @Column(nullable = false)
    var deactivated: Boolean
)