package com.ampnet.identityservice.config

import com.ampnet.identityservice.ManualFixedScheduler
import com.ampnet.identityservice.service.ScheduledExecutorServiceProvider
import com.ampnet.identityservice.service.impl.BlockchainQueueServiceImpl
import com.ampnet.identityservice.service.impl.FaucetQueueService
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestSchedulerConfiguration {

    @Bean
    fun whitelistQueueScheduler() = ManualFixedScheduler()

    @Bean
    fun faucetQueueScheduler() = ManualFixedScheduler()

    @Bean
    @Primary
    fun scheduledExecutorServiceProvider(
        whitelistQueueScheduler: ManualFixedScheduler,
        faucetQueueScheduler: ManualFixedScheduler
    ): ScheduledExecutorServiceProvider {
        return mock {
            given(it.newSingleThreadScheduledExecutor(BlockchainQueueServiceImpl.QUEUE_NAME))
                .willReturn(whitelistQueueScheduler)
            given(it.newSingleThreadScheduledExecutor(FaucetQueueService.QUEUE_NAME))
                .willReturn(faucetQueueScheduler)
        }
    }
}
