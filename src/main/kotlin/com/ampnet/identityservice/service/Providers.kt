package com.ampnet.identityservice.service

import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.UUID

interface UuidProvider {
    fun getUuid(): UUID
}

interface ZonedDateTimeProvider {
    fun getZonedDateTime(): ZonedDateTime
}

@Service
class RandomUuidProvider : UuidProvider {
    override fun getUuid(): UUID = UUID.randomUUID()
}

@Service
class CurrentZonedDateTimeProvider : ZonedDateTimeProvider {
    override fun getZonedDateTime(): ZonedDateTime = ZonedDateTime.now()
}
