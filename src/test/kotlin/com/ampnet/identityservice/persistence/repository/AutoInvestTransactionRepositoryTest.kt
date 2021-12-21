package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.persistence.model.AutoInvestTransaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@DataJpaTest
@ExtendWith(value = [SpringExtension::class])
@AutoConfigureTestDatabase
class AutoInvestTransactionRepositoryTest : TestBase() {

    @Autowired
    private lateinit var autoInvestTransactionRepository: AutoInvestTransactionRepository

    @Test
    fun mustCorrectlyFindAutoInvestTransactionByChainIdAndHash() {
        val tx = AutoInvestTransaction(
            UUID.randomUUID(),
            1L,
            "hash",
            ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
        )

        suppose("auto-invest transaction is inserted into database") {
            autoInvestTransactionRepository.saveAndFlush(tx)
        }

        verify("auto-invest transaction is correctly fetched by chain id and hash") {
            val databaseTx = autoInvestTransactionRepository.findByChainIdAndHash(tx.chainId, tx.hash)
            assertThat(databaseTx).isEqualTo(tx)
        }
    }
}
