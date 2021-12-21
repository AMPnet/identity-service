package com.ampnet.identityservice.persistence.model

import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import org.hibernate.annotations.Immutable
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Immutable
@Table(name = "auto_invest_transaction")
data class AutoInvestTransaction(
    @Id
    val uuid: UUID,

    @Column(nullable = false)
    val chainId: Long,

    @Column(nullable = false)
    val hash: String,

    @Column(nullable = false)
    val createdAt: ZonedDateTime
) {
    constructor(
        chainId: Long,
        hash: String,
        uuidProvider: UuidProvider,
        timeProvider: ZonedDateTimeProvider
    ) : this(
        uuidProvider.getUuid(),
        chainId,
        hash,
        timeProvider.getZonedDateTime()
    )

    override fun toString(): String {
        return "AutoInvestTransaction(uuid=$uuid, chainId=$chainId, hash='$hash', createdAt=$createdAt)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AutoInvestTransaction

        if (uuid != other.uuid) return false
        if (chainId != other.chainId) return false
        if (hash != other.hash) return false
        if (!createdAt.isEqual(other.createdAt)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + chainId.hashCode()
        result = 31 * result + hash.hashCode()
        result = 31 * result + createdAt.toEpochSecond().hashCode()
        return result
    }
}
