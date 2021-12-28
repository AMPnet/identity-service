package com.ampnet.identityservice.blockchain

import com.ampnet.identityservice.blockchain.IInvestService.InvestmentRecord
import com.ampnet.identityservice.blockchain.IInvestService.InvestmentRecordStatus

interface BlockchainService {
    fun whitelistAddress(addresses: List<String>, issuerAddress: String, chainId: Long): String?
    fun isMined(hash: String, chainId: Long): Boolean
    fun isWhitelisted(address: String, issuerAddress: String?, chainId: Long): Boolean
    fun sendFaucetFunds(addresses: List<String>, chainId: Long): String?
    fun getAutoInvestStatus(records: List<InvestmentRecord>, chainId: Long): List<InvestmentRecordStatus>
    fun autoInvestFor(records: List<InvestmentRecord>, chainId: Long): String?
}
