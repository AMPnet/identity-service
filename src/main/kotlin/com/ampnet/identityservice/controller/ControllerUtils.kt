package com.ampnet.identityservice.controller

import com.ampnet.core.jwt.exception.TokenException
import com.ampnet.identityservice.util.WalletAddress
import org.springframework.security.core.context.SecurityContextHolder

internal object ControllerUtils {

    fun getAddressFromSecurityContext(): WalletAddress =
        (SecurityContextHolder.getContext().authentication.principal as? String)?.let { WalletAddress(it) }
            ?: throw TokenException("SecurityContext authentication principal must be String")
}
