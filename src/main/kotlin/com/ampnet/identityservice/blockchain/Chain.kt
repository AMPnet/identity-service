package com.ampnet.identityservice.blockchain

@Suppress("MagicNumber")
enum class Chain(val id: Long, val rpcUrls: List<String>, val infura: String) {
    MATIC_MAIN(137, listOf("https://rpc-mainnet.matic.network/"), "https://polygon-mainnet.infura.io/v3/"),
    MATIC_TESTNET_MUMBAI(80001, listOf("https://rpc-mumbai.matic.today/"), "https://polygon-mumbai.infura.io/v3/"),
    ETHEREUM_MAIN(1, listOf("https://cloudflare-eth.com/"), "https://mainnet.infura.io/v3/");

    companion object {
        private val map = values().associateBy(Chain::id)
        fun fromId(type: Long) = map[type]
    }
}
