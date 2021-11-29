package com.ampnet.identityservice.persistence.model

import com.ampnet.identityservice.service.UuidProvider
import com.ampnet.identityservice.service.ZonedDateTimeProvider
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
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
    val amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: AutoInvestTaskStatus,

    @Column(nullable = false)
    val createdAt: ZonedDateTime
) {
    constructor(
        chainId: Long,
        userWalletAddress: String,
        campaignContractAddress: String,
        amount: BigDecimal,
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
        timeProvider.getZonedDateTime()
    )

    override fun toString(): String =
        "AutoInvestTask(uuid=$uuid, chainId=$chainId, userWalletAddress='$userWalletAddress'," +
            " campaignContractAddress='$campaignContractAddress', amount=$amount, status=$status," +
            " createdAt=$createdAt)"

    // thanks to the stupid-ass implementation of equals() for ZonedDateTime and BigDecimal, we need to override
    // equals() and hashCode() manually
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AutoInvestTask

        if (uuid != other.uuid) return false
        if (chainId != other.chainId) return false
        if (userWalletAddress != other.userWalletAddress) return false
        if (campaignContractAddress != other.campaignContractAddress) return false
        if (amount.compareTo(other.amount) != 0) return false
        if (status != other.status) return false
        if (!createdAt.isEqual(other.createdAt)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + chainId.hashCode()
        result = 31 * result + userWalletAddress.hashCode()
        result = 31 * result + campaignContractAddress.hashCode()
        result = 31 * result + amount.toDouble().hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + createdAt.toEpochSecond().hashCode()
        return result
    }
}

enum class AutoInvestTaskStatus {
    PENDING, IN_PROCESS
}
