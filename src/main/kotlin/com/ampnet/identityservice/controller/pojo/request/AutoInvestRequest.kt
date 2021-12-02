package com.ampnet.identityservice.controller.pojo.request

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigInteger

data class AutoInvestRequest(
    val campaignAddress: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val amount: BigInteger
)
