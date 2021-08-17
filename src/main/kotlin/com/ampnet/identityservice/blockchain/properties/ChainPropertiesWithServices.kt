package com.ampnet.identityservice.blockchain.properties

import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j

data class ChainPropertiesWithServices(
    val credentials: Credentials,
    val web3j: Web3j,
    val walletApproverAddress: String
)
