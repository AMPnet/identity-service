package com.ampnet.identityservice.config

import org.jobrunr.configuration.JobRunr
import org.jobrunr.configuration.JobRunrConfiguration
import org.jobrunr.jobs.filters.RetryFilter
import org.jobrunr.scheduling.JobScheduler
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import javax.sql.DataSource

@Configuration
// @Import(JobRunrConfiguration::class)
class JobRunrConfig(private val applicationProperties: ApplicationProperties) {

    @Bean
    fun initJobRunr(applicationContext: ApplicationContext): JobScheduler {
        return JobRunr
            .configure()
            .useStorageProvider(SqlStorageProviderFactory.using(applicationContext.getBean(DataSource::class.java)))
            .withJobFilter(RetryFilter(applicationProperties.retryPolicy.retryAttemptCount))
            .initialize()
    }
}
