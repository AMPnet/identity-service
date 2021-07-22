package com.ampnet.identityservice.service

import java.util.UUID

interface JobSchedulingService {

    fun enqueueTransaction(address: String): UUID
}
