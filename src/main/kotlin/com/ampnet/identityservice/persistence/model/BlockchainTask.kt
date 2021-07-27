package com.ampnet.identityservice.persistence.model

import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "blockchain_task")
class BlockchainTask(
    @Id
    val uuid: UUID,

    @Column(nullable = false)
    val payload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: BlockchainTaskStatus,

    @Column
    var hash: String?,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column
    var updatedAt: ZonedDateTime?
) {
    constructor(
        payload: String,
        uuidProvider: UuidProvider,
        timeProvider: ZonedDateTimeProvider
    ) : this(uuidProvider.getUuid(), payload, BlockchainTaskStatus.CREATED, null, timeProvider.getZonedDateTime(), null)

    override fun toString(): String =
        "BlockchainTask(uuid=$uuid, payload='$payload', status=$status, hash=$hash, createdAt=$createdAt, updatedAt=$updatedAt)"
}

enum class BlockchainTaskStatus {
    CREATED, IN_PROCESS, COMPLETED, FAILED
}
