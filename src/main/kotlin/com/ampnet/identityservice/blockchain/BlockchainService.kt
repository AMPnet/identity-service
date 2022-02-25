package com.ampnet.identityservice.blockchain

import com.ampnet.identityservice.blockchain.IInvestService.InvestmentRecord
import com.ampnet.identityservice.blockchain.IInvestService.InvestmentRecordStatus
import com.ampnet.identityservice.util.ChainId
import com.ampnet.identityservice.util.ContractAddress
import com.ampnet.identityservice.util.ContractVersion
import com.ampnet.identityservice.util.TransactionHash
import com.ampnet.identityservice.util.WalletAddress

interface BlockchainService {
    fun whitelistAddresses(
        addresses: List<WalletAddress>,
        issuerAddress: ContractAddress,
        chainId: ChainId
    ): TransactionHash?

    fun isMined(hash: TransactionHash, chainId: ChainId): Boolean
    fun isWhitelisted(address: WalletAddress, issuerAddress: ContractAddress?, chainId: ChainId): Boolean
    fun sendFaucetFunds(addresses: List<WalletAddress>, chainId: ChainId): TransactionHash?
    fun getAutoInvestStatus(records: List<InvestmentRecord>, chainId: ChainId): List<InvestmentRecordStatus>
    fun autoInvestFor(records: List<InvestmentRecordStatus>, chainId: ChainId): TransactionHash?
    fun getContractVersion(chainId: ChainId, address: ContractAddress): ContractVersion?
    fun isSignatureValid(chainId: ChainId, address: ContractAddress, data: ByteArray, signature: ByteArray): Boolean
}
