package com.ampnet.identityservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IdentityServiceApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<IdentityServiceApplication>(*args)
}
