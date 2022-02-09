package com.ampnet.identityservice.blockchain.properties

import com.ampnet.identityservice.util.ContractAddress
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j

data class ChainPropertiesWithServices(
    val walletApprover: CredentialsAndContractAddress,
    val faucet: CredentialsAndContractAddress?,
    val autoInvest: CredentialsAndContractAddress?,
    val web3j: Web3j,
    val blockTime: Long?
)

data class CredentialsAndContractAddress(
    val credentials: Credentials,
    val contractAddress: ContractAddress
)
