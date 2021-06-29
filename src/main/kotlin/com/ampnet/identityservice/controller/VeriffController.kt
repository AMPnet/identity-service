package com.ampnet.identityservice.controller

import com.ampnet.identityservice.controller.pojo.VeriffSessionRequest
import com.ampnet.userservice.exception.VeriffException
import com.ampnet.userservice.service.VeriffService
import com.ampnet.userservice.service.pojo.ServiceVerificationResponse
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import javax.servlet.http.HttpServletRequest

@RestController
class VeriffController(private val veriffService: VeriffService) {

    val userPayload = hashMapOf<String, String>() // publicAddres: payload()

//    Copy VeriffController from user-servicefor the start. Change VeriffController.getVeriffSession to accept POST with body: { "signed_payload": String } .
//    Verify signedPayload before getting session. Add enum: VerificationAction with value start_kyc for generating payload needed for this flow. Change the docs.
//
//    Copy VeriffService and change generating VeriffSession to use address instead of user UUID. See: VeriffSessionVerificationRequest .

    companion object : KLogging()

    @PostMapping("/veriff/session")
    fun getVeriffSession(@RequestBody request: VeriffSessionRequest): ResponseEntity<ServiceVerificationResponse> {
        // TODO verify signedPayload
        val user = ControllerUtils.getUserPrincipalFromSecurityContext().uuid
        val baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
            .replacePath(null)
            .build()
            .toUriString()
        logger.info { "Received request to get veriff session for user: $user" }
        return try {
            veriffService.getVeriffSession(user, baseUrl)?.let {
                return ResponseEntity.ok(it)
            }
            logger.warn("Could not get veriff session")
            ResponseEntity.status(HttpStatus.BAD_GATEWAY).build()
        } catch (ex: VeriffException) {
            logger.warn("Could not get veriff session", ex)
            ResponseEntity.status(HttpStatus.BAD_GATEWAY).build()
        }
    }

//    @PostMapping("/veriff/webhook/decision")
//    fun handleVeriffDecision(
//        @RequestBody data: String,
//        @RequestHeader("X-AUTH-CLIENT") client: String,
//        @RequestHeader("X-SIGNATURE") signature: String
//    ): ResponseEntity<Unit> {
//        logger.info { "Received Veriff decision" }
//        return try {
//            veriffService.verifyClient(client)
//            veriffService.verifySignature(signature, data)
//            val userInfo = veriffService.handleDecision(data)
//            if (userInfo == null) {
//                logger.info { "Veriff profile not approved. Veriff data: $data" }
//            } else {
//                logger.info { "Successfully verified Veriff session: ${userInfo.sessionId}" }
//            }
//            ResponseEntity.ok().build()
//        } catch (ex: VeriffException) {
//            logger.warn("Failed to handle Veriff decision webhook.", ex)
//            logger.info { "Veriff failed decision: $data" }
//            ResponseEntity.badRequest().build()
//        }
//    }
//
//    @PostMapping("/veriff/webhook/event")
//    fun handleVeriffEvent(
//        @RequestBody data: String,
//        @RequestHeader("X-AUTH-CLIENT") client: String,
//        @RequestHeader("X-SIGNATURE") signature: String
//    ): ResponseEntity<Unit> {
//        logger.info { "Received Veriff event" }
//        return try {
//            veriffService.verifyClient(client)
//            veriffService.verifySignature(signature, data)
//            val session = veriffService.handleEvent(data)
//            if (session == null) {
//                logger.info { "Missing Veriff session for event. Veriff data: $data" }
//                ResponseEntity.notFound().build()
//            } else {
//                logger.info { "Successfully updated Veriff session: ${session.id} for event: ${session.state.name}" }
//                ResponseEntity.ok().build()
//            }
//        } catch (ex: VeriffException) {
//            logger.warn("Failed to handle Veriff event webhook.", ex)
//            logger.info { "Veriff failed event: $data" }
//            ResponseEntity.badRequest().build()
//        }
//    }
}
