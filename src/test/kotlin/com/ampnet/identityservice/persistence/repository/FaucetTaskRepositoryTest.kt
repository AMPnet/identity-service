package com.ampnet.identityservice.persistence.repository

import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.persistence.model.FaucetTaskStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.ZonedDateTime
import java.util.UUID

@DataJpaTest
@ExtendWith(value = [SpringExtension::class])
@AutoConfigureTestDatabase
class FaucetTaskRepositoryTest : TestBase() {

    @Autowired
    private lateinit var faucetTaskRepository: FaucetTaskRepository

    @Test
    fun mustCorrectlyAddAndFlushAddressQueue() {
        val chain1Addresses = listOf("addr1", "addr2", "addr3", "addr1", "addr4", "addr2")
        val chain2Addresses = listOf("addr1", "addr4", "addr5")

        suppose("some addresses are added to the queue") {
            chain1Addresses.forEach { faucetTaskRepository.addAddressToQueue(it, 1L) }
            chain2Addresses.forEach { faucetTaskRepository.addAddressToQueue(it, 2L) }
        }

        val task1Uuid = UUID.randomUUID()
        val now = ZonedDateTime.now()

        suppose("address queue is flushed for chain 1") {
            faucetTaskRepository.flushAddressQueueForChainId(task1Uuid, 1L, now, 100)
        }

        val uniqueChain1Addresses = chain1Addresses.toSet()

        verify("task is created for flushed addresses for chain 1") {
            val task = faucetTaskRepository.findById(task1Uuid)
            assertThat(task).hasValueSatisfying {
                assertThat(it.addresses).containsExactlyInAnyOrderElementsOf(uniqueChain1Addresses)
                assertThat(it.chainId).isEqualTo(1L)
                assertThat(it.status).isEqualTo(FaucetTaskStatus.CREATED)
            }
        }

        val task2Uuid = UUID.randomUUID()

        suppose("address queue is flushed for chain 2") {
            faucetTaskRepository.flushAddressQueueForChainId(task2Uuid, 2L, now, 100)
        }

        val uniqueChain2Addresses = chain2Addresses.toSet()

        verify("task is created for flushed addresses for chain 2") {
            val task = faucetTaskRepository.findById(task2Uuid)
            assertThat(task).hasValueSatisfying {
                assertThat(it.addresses).containsExactlyInAnyOrderElementsOf(uniqueChain2Addresses)
                assertThat(it.chainId).isEqualTo(2L)
                assertThat(it.status).isEqualTo(FaucetTaskStatus.CREATED)
            }
        }

        val nextChain1Addresses = listOf("addr6", "addr7", "addr8")

        suppose("more addresses are added to the queue for chain 1") {
            nextChain1Addresses.forEach { faucetTaskRepository.addAddressToQueue(it, 1L) }
        }

        val task3Uuid = UUID.randomUUID()

        suppose("address queue is flushed for chain 1") {
            faucetTaskRepository.flushAddressQueueForChainId(task3Uuid, 1L, now, 100)
        }

        val uniqueNextChain1Addresses = nextChain1Addresses.toSet()

        verify("task is created for flushed addresses for chain 1") {
            val task = faucetTaskRepository.findById(task3Uuid)
            assertThat(task).hasValueSatisfying {
                assertThat(it.addresses).containsExactlyInAnyOrderElementsOf(uniqueNextChain1Addresses)
                assertThat(it.chainId).isEqualTo(1L)
                assertThat(it.status).isEqualTo(FaucetTaskStatus.CREATED)
            }
        }
    }

    @Test
    fun mustCorrectlyAddAndFlushAddressQueueForLimitedNumberOfAddressesPerTask() {
        val chain1Addresses = listOf("addr1", "addr2", "addr3", "addr4", "addr5", "addr6")
        val chain2Addresses = listOf("addr1", "addr4", "addr5")

        suppose("some addresses are added to the queue") {
            chain1Addresses.forEach { faucetTaskRepository.addAddressToQueue(it, 1L) }
            chain2Addresses.forEach { faucetTaskRepository.addAddressToQueue(it, 2L) }
        }

        val task1Uuid = UUID.randomUUID()
        val now = ZonedDateTime.now()

        suppose("address queue is flushed for chain 1") {
            faucetTaskRepository.flushAddressQueueForChainId(task1Uuid, 1L, now, 2)
        }

        val remainingChain1Addresses = chain1Addresses.toMutableList()
        verify("task is created for only 2 flushed addresses for chain 1") {
            val task = faucetTaskRepository.findById(task1Uuid)
            assertThat(task).hasValueSatisfying {
                assertThat(it.addresses.toSet()).hasSize(2)
                assertThat(it.addresses[0]).isIn(remainingChain1Addresses)
                assertThat(it.addresses[1]).isIn(remainingChain1Addresses)
                assertThat(it.chainId).isEqualTo(1L)
                assertThat(it.status).isEqualTo(FaucetTaskStatus.CREATED)

                remainingChain1Addresses.removeAll(it.addresses.toSet())
                assertThat(remainingChain1Addresses).hasSize(chain1Addresses.size - 2)
            }
        }

        val task2Uuid = UUID.randomUUID()
        suppose("address queue is flushed for chain 1") {
            faucetTaskRepository.flushAddressQueueForChainId(task2Uuid, 1L, now, 2)
        }

        verify("another task is created for only 2 flushed addresses for chain 1") {
            val task = faucetTaskRepository.findById(task2Uuid)
            assertThat(task).hasValueSatisfying {
                assertThat(it.addresses.toSet()).hasSize(2)
                assertThat(it.addresses[0]).isIn(remainingChain1Addresses)
                assertThat(it.addresses[1]).isIn(remainingChain1Addresses)
                assertThat(it.chainId).isEqualTo(1L)
                assertThat(it.status).isEqualTo(FaucetTaskStatus.CREATED)

                remainingChain1Addresses.removeAll(it.addresses.toSet())
                assertThat(remainingChain1Addresses).hasSize(chain1Addresses.size - 4)
            }
        }

        val task3Uuid = UUID.randomUUID()
        suppose("address queue is flushed for chain 1") {
            faucetTaskRepository.flushAddressQueueForChainId(task3Uuid, 1L, now, 2)
        }

        verify("another task is created for only 2 flushed addresses for chain 1") {
            val task = faucetTaskRepository.findById(task3Uuid)
            assertThat(task).hasValueSatisfying {
                assertThat(it.addresses.toSet()).hasSize(2)
                assertThat(it.addresses[0]).isIn(remainingChain1Addresses)
                assertThat(it.addresses[1]).isIn(remainingChain1Addresses)
                assertThat(it.chainId).isEqualTo(1L)
                assertThat(it.status).isEqualTo(FaucetTaskStatus.CREATED)

                remainingChain1Addresses.removeAll(it.addresses.toSet())
                assertThat(remainingChain1Addresses).isEmpty()
            }
        }

        val task4Uuid = UUID.randomUUID()
        suppose("address queue is flushed for chain 2") {
            faucetTaskRepository.flushAddressQueueForChainId(task4Uuid, 2L, now, 2)
        }

        val remainingChain2Addresses = chain2Addresses.toMutableList()
        verify("task is created for only 2 flushed addresses for chain 2") {
            val task = faucetTaskRepository.findById(task4Uuid)
            assertThat(task).hasValueSatisfying {
                assertThat(it.addresses.toSet()).hasSize(2)
                assertThat(it.addresses[0]).isIn(remainingChain2Addresses)
                assertThat(it.addresses[1]).isIn(remainingChain2Addresses)
                assertThat(it.chainId).isEqualTo(2L)
                assertThat(it.status).isEqualTo(FaucetTaskStatus.CREATED)

                remainingChain2Addresses.removeAll(it.addresses.toSet())
                assertThat(remainingChain2Addresses).hasSize(chain2Addresses.size - 2)
            }
        }

        val task5Uuid = UUID.randomUUID()
        suppose("address queue is flushed for chain 2") {
            faucetTaskRepository.flushAddressQueueForChainId(task5Uuid, 2L, now, 2)
        }

        verify("another task is created for only 1 flushed address for chain 2") {
            val task = faucetTaskRepository.findById(task5Uuid)
            assertThat(task).hasValueSatisfying {
                assertThat(it.addresses.toSet()).hasSize(1)
                assertThat(it.addresses[0]).isIn(remainingChain2Addresses)
                assertThat(it.chainId).isEqualTo(2L)
                assertThat(it.status).isEqualTo(FaucetTaskStatus.CREATED)

                remainingChain2Addresses.removeAll(it.addresses.toSet())
                assertThat(remainingChain2Addresses).isEmpty()
            }
        }
    }

    @Test
    fun mustCorrectlyFetchChainIdsWithPendingAddresses() {
        verify("there are no chains with pending addresses") {
            assertThat(faucetTaskRepository.fetchChainIdsWithPendingAddresses()).isEmpty()
        }

        suppose("some addresses are added to the queue for chain 1") {
            faucetTaskRepository.addAddressToQueue("addr1", 1L)
            faucetTaskRepository.addAddressToQueue("addr2", 1L)
            faucetTaskRepository.addAddressToQueue("addr3", 1L)
        }

        verify("chain 1 is returned as chain with pending addresses") {
            assertThat(faucetTaskRepository.fetchChainIdsWithPendingAddresses()).isEqualTo(listOf(1L))
        }

        suppose("single addresses is added to the queue for chain 2") {
            faucetTaskRepository.addAddressToQueue("addr1", 2L)
        }

        verify("chain 1 and chain 2 are returned as chains with pending addresses") {
            assertThat(faucetTaskRepository.fetchChainIdsWithPendingAddresses()).containsExactlyInAnyOrder(1L, 2L)
        }
    }
}
