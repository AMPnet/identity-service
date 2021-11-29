package com.ampnet.identityservice.controller.pojo.response

import java.math.BigDecimal

data class AutoInvestResponse(
    val walletAddress: String,
    val campaignAddress: String,
    val amount: BigDecimal
)
