package com.ampnet.identityservice.persistence.model

import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import com.vladmihalcea.hibernate.type.array.StringArrayType
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
    var payload: String?,

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
        timeProvider: ZonedDateTimeProvider,
        payload: String? = null
    ) : this(
        uuidProvider.getUuid(),
        addresses,
        chainId,
        FaucetTaskStatus.CREATED,
        payload,
        null,
        timeProvider.getZonedDateTime(),
        null
    )

    override fun toString(): String =
        "FaucetTask(uuid=$uuid, addresses=${addresses.contentToString()}, chainId=$chainId, status=$status," +
            " hash=$hash, createdAt=$createdAt, updatedAt=$updatedAt)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaucetTask

        if (uuid != other.uuid) return false
        if (!addresses.contentEquals(other.addresses)) return false
        if (chainId != other.chainId) return false
        if (status != other.status) return false
        if (payload != other.payload) return false
        if (hash != other.hash) return false
        if (!createdAt.isEqual(other.createdAt)) return false
        if (updatedAt?.isEqual(other.updatedAt)?.not() == true) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + addresses.contentHashCode()
        result = 31 * result + chainId.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + (payload?.hashCode() ?: 0)
        result = 31 * result + (hash?.hashCode() ?: 0)
        result = 31 * result + createdAt.toEpochSecond().hashCode()
        result = 31 * result + (updatedAt?.toEpochSecond()?.hashCode() ?: 0)
        return result
    }
}

enum class FaucetTaskStatus {
    CREATED, IN_PROCESS, COMPLETED, FAILED
}
