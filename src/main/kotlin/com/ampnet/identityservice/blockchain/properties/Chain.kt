package com.ampnet.identityservice.blockchain.properties

@Suppress("MagicNumber")
enum class Chain(val id: Long, val rpcUrl: String, val infura: String?, val priceFeed: String?) {
    MATIC_MAIN(
        137,
        "https://rpc-mainnet.matic.network/",
        "https://polygon-mainnet.infura.io/v3/",
        "https://gasstation-mainnet.matic.network"
    ),
    MATIC_TESTNET_MUMBAI(
        80001,
        "https://rpc-mumbai.matic.today/",
        "https://polygon-mumbai.infura.io/v3/",
        "https://gasstation-mumbai.matic.today"
    ),
    ETHEREUM_MAIN(
        1,
        "https://cloudflare-eth.com/",
        "https://mainnet.infura.io/v3/",
        null
    ),
    HARDHAT_TESTNET(
        31337,
        "http://hardhat:8545",
        null,
        null
    );

    companion object {
        private val map = values().associateBy(Chain::id)
        fun fromId(type: Long) = map[type]
    }
}
