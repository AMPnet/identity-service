package com.ampnet.identityservice.persistence.model

import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InternalException
import com.ampnet.identityservice.service.pojo.VeriffDocument
import com.ampnet.identityservice.service.pojo.VeriffPerson
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "user_info")
@Suppress("LongParameterList")
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
) {
    constructor(sessionId: String, person: VeriffPerson, document: VeriffDocument) : this(
        UUID.randomUUID(),
        sessionId,
        person.firstName ?: throw InternalException(ErrorCode.REG_VERIFF, "Missing first name from veriff"),
        person.lastName ?: throw InternalException(ErrorCode.REG_VERIFF, "Missing last name from veriff"),
        person.idNumber,
        person.dateOfBirth,
        Document(document),
        person.nationality,
        person.placeOfBirth,
        ZonedDateTime.now(),
        false,
        false,
    )
}
