package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.service.JobSchedulingService
import com.ampnet.identityservice.service.Web3jService
import org.jobrunr.scheduling.JobScheduler
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class JobSchedulingServiceImpl(
    private val jobScheduler: JobScheduler,
    private val web3jService: Web3jService
) : JobSchedulingService {

    override fun enqueueTransaction(address: String): UUID {
        return jobScheduler.enqueue { web3jService.approveWallet(address) }.asUUID()
    }
}
