package com.ampnet.identityservice.controller.pojo.response

import com.ampnet.identityservice.persistence.model.AutoInvestTask
import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigInteger

data class AutoInvestResponse(
    val walletAddress: String,
    val campaignAddress: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val amount: BigInteger
) {
    constructor(task: AutoInvestTask) : this(
        walletAddress = task.userWalletAddress,
        campaignAddress = task.campaignContractAddress,
        amount = task.amount
    )
}

data class AutoInvestListResponse(val autoInvests: List<AutoInvestResponse>)
