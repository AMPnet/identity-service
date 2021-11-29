package com.ampnet.identityservice.controller.pojo.request

import java.math.BigDecimal

data class AutoInvestRequest(
    val campaignAddress: String,
    val amount: BigDecimal
)
