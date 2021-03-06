package com.ampnet.identityservice.persistence.model

import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import org.hibernate.annotations.Immutable
import java.math.BigInteger
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Immutable
@Table(name = "auto_invest_task")
data class AutoInvestTask(
    @Id
    val uuid: UUID,

    @Column(nullable = false)
    val chainId: Long,

    @Column(nullable = false)
    val userWalletAddress: String,

    @Column(nullable = false)
    val campaignContractAddress: String,

    @Column(nullable = false)
    val amount: BigInteger,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: AutoInvestTaskStatus,

    @Column(nullable = true)
    val hash: String?,

    @Column(nullable = false)
    val createdAt: ZonedDateTime
) {
    constructor(
        chainId: Long,
        userWalletAddress: String,
        campaignContractAddress: String,
        amount: BigInteger,
        status: AutoInvestTaskStatus,
        uuidProvider: UuidProvider,
        timeProvider: ZonedDateTimeProvider
    ) : this(
        uuidProvider.getUuid(),
        chainId,
        userWalletAddress,
        campaignContractAddress,
        amount,
        status,
        null,
        timeProvider.getZonedDateTime()
    )

    override fun toString(): String =
        "AutoInvestTask(uuid=$uuid, chainId=$chainId, userWalletAddress='$userWalletAddress'," +
            " campaignContractAddress='$campaignContractAddress', amount=$amount, status=$status," +
            " hash=$hash, createdAt=$createdAt)"

    // thanks to the stupid-ass implementation of equals() for ZonedDateTime, we need to override
    // equals() and hashCode() manually
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AutoInvestTask

        if (uuid != other.uuid) return false
        if (chainId != other.chainId) return false
        if (userWalletAddress != other.userWalletAddress) return false
        if (campaignContractAddress != other.campaignContractAddress) return false
        if (amount != other.amount) return false
        if (status != other.status) return false
        if (hash != other.hash) return false
        if (!createdAt.isEqual(other.createdAt)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + chainId.hashCode()
        result = 31 * result + userWalletAddress.hashCode()
        result = 31 * result + campaignContractAddress.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + hash.hashCode()
        result = 31 * result + createdAt.toEpochSecond().hashCode()
        return result
    }
}

enum class AutoInvestTaskStatus {
    PENDING, IN_PROCESS
}

enum class AutoInvestTaskHistoryStatus {
    SUCCESS, FAILURE, EXPIRED
}
