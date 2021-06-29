package com.ampnet.identityservice.service

import java.util.Optional

internal object ServiceUtils {
    fun <T> wrapOptional(optional: Optional<T>): T? {
        return if (optional.isPresent) optional.get() else null
    }
}
