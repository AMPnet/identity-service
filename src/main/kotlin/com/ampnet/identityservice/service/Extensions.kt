package com.ampnet.identityservice.service

import java.util.Optional

fun <T> Optional<T>.unwrap(): T? = if (this.isPresent) this.get() else null
