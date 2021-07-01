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
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import javax.servlet.http.HttpServletRequest

@RestController
class VeriffController(
    private val veriffService: VeriffService,
    private val verificationService: VerificationService
) {

    companion object : KLogging()

    @Suppress("ReturnCount")
    @PostMapping("/veriff/session")
    fun getVeriffSession(
        @RequestBody request: VeriffRequest,
        servlet: HttpServletRequest
    ): ResponseEntity<ServiceVerificationResponse> {
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

    @PostMapping("/veriff/webhook/decision")
    fun handleVeriffDecision(
        @RequestBody data: String,
        @RequestHeader("X-AUTH-CLIENT") client: String,
        @RequestHeader("X-SIGNATURE") signature: String
    ): ResponseEntity<Unit> {
        logger.info { "Received Veriff decision" }
        return try {
            veriffService.verifyClient(client)
            veriffService.verifySignature(signature, data)
            val userInfo = veriffService.handleDecision(data)
            if (userInfo == null) {
                logger.info { "Veriff profile not approved. Veriff data: $data" }
            } else {
                logger.info { "Successfully verified Veriff session: ${userInfo.sessionId}" }
            }
            ResponseEntity.ok().build()
        } catch (ex: VeriffException) {
            logger.warn("Failed to handle Veriff decision webhook.", ex)
            logger.info { "Veriff failed decision: $data" }
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/veriff/webhook/event")
    fun handleVeriffEvent(
        @RequestBody data: String,
        @RequestHeader("X-AUTH-CLIENT") client: String,
        @RequestHeader("X-SIGNATURE") signature: String
    ): ResponseEntity<Unit> {
        logger.info { "Received Veriff event" }
        return try {
            veriffService.verifyClient(client)
            veriffService.verifySignature(signature, data)
            val session = veriffService.handleEvent(data)
            if (session == null) {
                logger.info { "Missing Veriff session for event. Veriff data: $data" }
                ResponseEntity.notFound().build()
            } else {
                logger.info { "Successfully updated Veriff session: ${session.id} for event: ${session.state.name}" }
                ResponseEntity.ok().build()
            }
        } catch (ex: VeriffException) {
            logger.warn("Failed to handle Veriff event webhook.", ex)
            logger.info { "Veriff failed event: $data" }
            ResponseEntity.badRequest().build()
        }
    }
}
