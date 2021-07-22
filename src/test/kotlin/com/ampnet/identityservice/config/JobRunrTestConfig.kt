package com.ampnet.identityservice.config

import org.jobrunr.configuration.JobRunrConfiguration
import org.jobrunr.jobs.mappers.JobMapper
import org.jobrunr.storage.InMemoryStorageProvider
import org.jobrunr.storage.StorageProvider
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@TestConfiguration
@Import(JobRunrConfiguration::class)
class JobRunrTestConfig {

    @Bean
    fun storageProvider(jobMapper: JobMapper): StorageProvider {
        val storageProvider = InMemoryStorageProvider()
        storageProvider.setJobMapper(jobMapper)
        return storageProvider
    }
}
