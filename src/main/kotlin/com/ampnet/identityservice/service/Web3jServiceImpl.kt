package com.ampnet.identityservice.service

import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.contract.IIssuer
import mu.KLogging
import org.jobrunr.jobs.annotations.Job
import org.springframework.stereotype.Service
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.DefaultGasProvider

@Service
class Web3jServiceImpl(
    private val applicationProperties: ApplicationProperties,
) : Web3jService {

    companion object : KLogging()

    private val web3j by lazy { Web3j.build(HttpService(applicationProperties.provider.blockchainApi)) }
    private val credentials by lazy { Credentials.create(applicationProperties.smartContract.privateKey) }
    private class Contract(contractAddress: String, web3j: Web3j, credentials: Credentials) : IIssuer(
        contractAddress, web3j, credentials, DefaultGasProvider()
    )

    private val contract by lazy {
        Contract(applicationProperties.smartContract.issuerContractAddress, web3j, credentials)
    }

    @Job(name = "Approve wallet")
    override fun approveWallet(address: String): String {
        return contract.approveWallet(address).send().blockHash
    }

    override fun isWalletApproved(address: String): Boolean {
        return contract.isWalletApproved(address).send()
    }
}
