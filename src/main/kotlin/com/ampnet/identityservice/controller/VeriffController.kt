package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.VeriffRequest
import com.ampnet.identityservice.exception.VeriffException
import com.ampnet.identityservice.service.VeriffService
import com.ampnet.identityservice.service.VerificationService
import com.ampnet.identityservice.service.pojo.ServiceVerificationResponse
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import javax.servlet.http.HttpServletRequest

@RestController
class VeriffController(
    private val veriffService: VeriffService,
    private val verificationService: VerificationService
    ) {

    companion object : KLogging()

    @PostMapping("/veriff/session")
    fun getVeriffSession(servlet: HttpServletRequest, @RequestBody request: VeriffRequest): ResponseEntity<ServiceVerificationResponse> {
        val address = ControllerUtils.getAddressFromSecurityContext()
        val baseUrl = ServletUriComponentsBuilder.fromRequestUri(servlet)
            .replacePath(null)
            .build()
            .toUriString()
        logger.info { "Received request to get veriff session for address: $address" }
        val payloadValid = verificationService.verifyPayload(address, request.signedPayload)
        if (payloadValid.not()) return ResponseEntity.badRequest().build()
        return try {
            veriffService.getVeriffSession(address, baseUrl)?.let {
                return ResponseEntity.ok(it)
            }
            logger.warn("Could not get veriff session")
            ResponseEntity.status(HttpStatus.BAD_GATEWAY).build()
        } catch (ex: VeriffException) {
            logger.warn("Could not get veriff session", ex)
            ResponseEntity.status(HttpStatus.BAD_GATEWAY).build()
        }
    }
}
