package com.ampnet.identityservice.controller

import com.ampnet.core.jwt.exception.TokenException
import org.springframework.security.core.context.SecurityContextHolder

internal object ControllerUtils {

    fun getAddressFromSecurityContext(): String =
        (SecurityContextHolder.getContext().authentication.principal as? String)?.lowercase()
            ?: throw TokenException("SecurityContext authentication principal must be String")
}
