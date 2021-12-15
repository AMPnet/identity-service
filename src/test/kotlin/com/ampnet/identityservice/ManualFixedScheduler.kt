package com.ampnet.identityservice

import com.ampnet.identityservice.service.FixedScheduler
import java.util.concurrent.TimeUnit

class ManualFixedScheduler : FixedScheduler {

    private var command: Runnable? = null

    override fun scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit) {
        this.command = command
    }

    override fun shutdown() {}

    fun execute() = command?.run()
}
