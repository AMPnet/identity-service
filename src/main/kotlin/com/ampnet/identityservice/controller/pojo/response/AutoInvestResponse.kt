package com.ampnet.identityservice.controller.pojo.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigInteger

data class AutoInvestResponse(
    val walletAddress: String,
    val campaignAddress: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val amount: BigInteger
)
