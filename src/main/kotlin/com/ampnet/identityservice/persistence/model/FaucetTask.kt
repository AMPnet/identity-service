package com.ampnet.identityservice.persistence.model

import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import com.vladmihalcea.hibernate.type.array.StringArrayType
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@TypeDefs(
    TypeDef(name = "string-array", typeClass = StringArrayType::class)
)
@Entity
@Table(name = "faucet_task")
@Immutable
class FaucetTask(
    @Id
    val uuid: UUID,

    @Type(type = "string-array")
    @Column(
        name = "addresses",
        columnDefinition = "varchar[]",
        nullable = false
    )
    val addresses: Array<String>,

    @Column(nullable = false)
    val chainId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FaucetTaskStatus,

    @Column
    var hash: String?,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column
    var updatedAt: ZonedDateTime?
) {
    constructor(
        addresses: Array<String>,
        chainId: Long,
        uuidProvider: UuidProvider,
        timeProvider: ZonedDateTimeProvider
    ) : this(
        uuidProvider.getUuid(),
        addresses,
        chainId,
        FaucetTaskStatus.CREATED,
        null,
        timeProvider.getZonedDateTime(),
        null
    )
}

enum class FaucetTaskStatus {
    CREATED, IN_PROCESS, COMPLETED, FAILED
}
