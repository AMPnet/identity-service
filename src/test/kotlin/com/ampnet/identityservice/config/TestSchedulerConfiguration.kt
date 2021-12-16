package com.ampnet.identityservice.config

import com.ampnet.identityservice.ManualFixedScheduler
import com.ampnet.identityservice.service.ScheduledExecutorServiceProvider
import com.ampnet.identityservice.service.impl.AutoInvestQueueServiceImpl
import com.ampnet.identityservice.service.impl.BlockchainQueueServiceImpl
import com.ampnet.identityservice.service.impl.FaucetQueueService
import mu.KLogging
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestSchedulerConfiguration {

    companion object : KLogging()

    @Bean
    fun whitelistQueueScheduler() = ManualFixedScheduler()

    @Bean
    fun faucetQueueScheduler() = ManualFixedScheduler()

    @Bean
    fun autoInvestQueueScheduler() = ManualFixedScheduler()

    @Bean
    @Primary
    fun scheduledExecutorServiceProvider(
        whitelistQueueScheduler: ManualFixedScheduler,
        faucetQueueScheduler: ManualFixedScheduler,
        autoInvestQueueScheduler: ManualFixedScheduler
    ): ScheduledExecutorServiceProvider {
        logger.info { "Using manual schedulers for tests" }
        return mock {
            given(it.newSingleThreadScheduledExecutor(BlockchainQueueServiceImpl.QUEUE_NAME))
                .willReturn(whitelistQueueScheduler)
            given(it.newSingleThreadScheduledExecutor(FaucetQueueService.QUEUE_NAME))
                .willReturn(faucetQueueScheduler)
            given(it.newSingleThreadScheduledExecutor(AutoInvestQueueServiceImpl.QUEUE_NAME))
                .willReturn(autoInvestQueueScheduler)
        }
    }
}
