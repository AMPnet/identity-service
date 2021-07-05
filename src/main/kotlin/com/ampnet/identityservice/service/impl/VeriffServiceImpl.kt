package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.exception.ResourceNotFoundException
import com.ampnet.identityservice.exception.VeriffException
import com.ampnet.identityservice.exception.VeriffReasonCode
import com.ampnet.identityservice.exception.VeriffVerificationCode
import com.ampnet.identityservice.persistence.model.UserInfo
import com.ampnet.identityservice.persistence.model.VeriffDecision
import com.ampnet.identityservice.persistence.model.VeriffSession
import com.ampnet.identityservice.persistence.model.VeriffSessionState
import com.ampnet.identityservice.persistence.repository.UserInfoRepository
import com.ampnet.identityservice.persistence.repository.VeriffDecisionRepository
import com.ampnet.identityservice.persistence.repository.VeriffSessionRepository
import com.ampnet.identityservice.service.ServiceUtils
import com.ampnet.identityservice.service.UserService
import com.ampnet.identityservice.service.VeriffService
import com.ampnet.identityservice.service.pojo.ServiceVerificationResponse
import com.ampnet.identityservice.service.pojo.VeriffDocument
import com.ampnet.identityservice.service.pojo.VeriffEvent
import com.ampnet.identityservice.service.pojo.VeriffEventAction
import com.ampnet.identityservice.service.pojo.VeriffPerson
import com.ampnet.identityservice.service.pojo.VeriffResponse
import com.ampnet.identityservice.service.pojo.VeriffSessionRequest
import com.ampnet.identityservice.service.pojo.VeriffSessionResponse
import com.ampnet.identityservice.service.pojo.VeriffStatus
import com.ampnet.identityservice.service.pojo.VeriffVerification
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import java.net.URI
import java.security.MessageDigest
import java.time.ZonedDateTime

@Service
@Suppress("TooManyFunctions", "ReturnCount")
class VeriffServiceImpl(
    private val veriffSessionRepository: VeriffSessionRepository,
    private val veriffDecisionRepository: VeriffDecisionRepository,
    private val userInfoRepository: UserInfoRepository,
    private val applicationProperties: ApplicationProperties,
    private val userService: UserService,
    private val restTemplate: RestTemplate,
    @Qualifier("camelCaseObjectMapper") private val objectMapper: ObjectMapper
) : VeriffService {

    companion object : KLogging()

    /**
     * Service returns to the user Veriff session data depending on the user state in the session.
     * For non existing session, new Veriff session is generated.
     * If the user started a session and still waiting for the decision (decision is missing) from Veriff,
     * current session is still in process and returned to the user.
     * If the decision is received from Veriff, response depends on the decision status:
     * for `declined`, `abandoned` or `expired` new Veriff session is generated and returned with current decision,
     * for other statuses current session is return with current decision.
     * For more info see: <a href="https://developers.veriff.com/#session-status-diagram">Veriff diagram</a>
     * If the session is older than 7 days, create new session.
     *
     * @param address String of the user who initiated Veriff session flow.
     * @param baseUrl String of the url from which request came from.
     * @return ServiceVerificationResponse containing Veriff session verificationUrl and state as mandatory data,
     * `decision` is null until Veriff sends the data to webhook. Null response is returned if
     * the new Veriff session cannot be created.
     * @throws ResourceNotFoundException if the address is missing.
     */
    @Transactional
    @Throws(ResourceNotFoundException::class)
    override fun getVeriffSession(address: String, baseUrl: String): ServiceVerificationResponse? {
        logger.debug { "Get Veriff session for address: $address" }
        val session = veriffSessionRepository.findByUserAddressOrderByCreatedAtDesc(address).firstOrNull()
            ?: return createVeriffSession(address, baseUrl)?.let { newSession ->
                ServiceVerificationResponse(newSession.url, newSession.state)
            }
        logger.debug { "User has pending Veriff session" }

        @Suppress("MagicNumber")
        if (session.createdAt.isBefore(ZonedDateTime.now().minusDays(7))) {
            logger.warn { "Veriff session expired" }
            createVeriffSession(address, baseUrl)?.let { newSession ->
                return ServiceVerificationResponse(newSession.url, newSession.state)
            }
        }

        val decision = ServiceUtils.wrapOptional(veriffDecisionRepository.findById(session.id))
            ?: return ServiceVerificationResponse(session.url, session.state)
        logger.debug { "User session: ${session.id} has decision: ${decision.status.name}" }

        return when (decision.status) {
            VeriffStatus.approved, VeriffStatus.resubmission_requested, VeriffStatus.review ->
                ServiceVerificationResponse(session.url, session.state, decision)
            VeriffStatus.declined, VeriffStatus.abandoned, VeriffStatus.expired ->
                createVeriffSession(address, baseUrl)?.let { newSession ->
                    ServiceVerificationResponse(newSession.url, newSession.state, decision)
                }
        }
    }

    override fun handleDecision(data: String): UserInfo? {
        val response = mapVeriffResponse(data)
        val verification = response.verification
            ?: throw VeriffException("Missing verification data. Status: ${response.status}")
        val decision = VeriffDecision(verification)
        veriffDecisionRepository.save(decision)
        getVeriffPerson(verification)?.let { person ->
            val document = getVeriffDocument(verification)
            val userInfo = UserInfo(verification.id, person, document)
            userInfoRepository.save(userInfo)
            logger.info { "Successfully created user info: ${userInfo.uuid}" }
            verifyUser(userInfo, verification.vendorData)
            return userInfo
        }
        return null
    }

    @Throws(VeriffException::class)
    @Transactional
    override fun handleEvent(data: String): VeriffSession? {
        try {
            val event: VeriffEvent = objectMapper.readValue(data)
            val veriffSession = ServiceUtils.wrapOptional(veriffSessionRepository.findById(event.id))
            return if (veriffSession == null) {
                logger.info { "Missing veriff session for event: $event" }
                null
            } else {
                veriffSession.state = when (event.action) {
                    VeriffEventAction.started -> VeriffSessionState.STARTED
                    VeriffEventAction.submitted -> VeriffSessionState.SUBMITTED
                }
                if (veriffSession.state == VeriffSessionState.STARTED) {
                    veriffDecisionRepository.deleteByIdIfPresent(veriffSession.id)
                }
                veriffSession
            }
        } catch (ex: JsonProcessingException) {
            throw VeriffException("Could not map Veriff event", ex)
        }
    }

    @Throws(VeriffException::class)
    override fun verifyClient(client: String) {
        if (client != applicationProperties.veriff.apiKey) throw VeriffException("X-AUTH-CLIENT: $client is invalid")
    }

    @Throws(VeriffException::class)
    override fun verifySignature(signature: String, data: String) {
        val hexHash = generateSignature(data)
        if (signature != hexHash) {
            throw VeriffException("X-SIGNATURE invalid!")
        }
    }

    private fun createVeriffSession(address: String, baseUrl: String): VeriffSession? {
        logger.debug { "Creating Veriff session for address: $address" }
        val user = userService.getUser(address)
        val callback = if (baseUrl.startsWith("https:")) baseUrl else ""
        logger.debug { "Callback url for Veriff: $callback. Base url: $baseUrl" }
        val request = objectMapper.writeValueAsString(VeriffSessionRequest(user, callback))
        val signature = generateSignature(request)
        val headers = generateVeriffHeaders(signature)
        val httpEntity = HttpEntity(request, headers)
        val uri = URI(applicationProperties.veriff.baseUrl + "/v1/sessions/")
        return try {
            val veriffSessionResponse = restTemplate.postForEntity<VeriffSessionResponse>(uri, httpEntity).body
            veriffSessionResponse?.let {
                val veriffSession = VeriffSession(veriffSessionResponse, address)
                logger.debug { "Create Veriff session: ${veriffSession.id}" }
                return veriffSessionRepository.save(veriffSession)
            }
        } catch (ex: RestClientException) {
            logger.warn("Could not create Veriff session", ex)
            null
        }
    }

    private fun generateSignature(payload: String): String {
        val request = payload + applicationProperties.veriff.privateKey
        val hash = MessageDigest.getInstance("SHA-256").digest(request.toByteArray())
        return bytesToHex(hash)
    }

    private fun generateVeriffHeaders(signature: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.set("X-AUTH-CLIENT", applicationProperties.veriff.apiKey)
        headers.set("X-SIGNATURE", signature)
        return headers
    }

    @Suppress("MagicNumber")
    private fun bytesToHex(hash: ByteArray): String {
        val hexString = StringBuilder(2 * hash.size)
        for (i in hash.indices) {
            val hex = Integer.toHexString(0xff and hash[i].toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }

    private fun getVeriffPerson(verification: VeriffVerification): VeriffPerson? {
        val status = verification.status
        val code = VeriffVerificationCode.fromInt(verification.code)
        val reason = VeriffReasonCode.fromInt(verification.reasonCode)
        val message = "Status: $status with code ${code?.code} - ${code?.name}. " +
            "Reason code: ${reason?.code} - ${reason?.name}. Reason: ${verification.reason}. " +
            "Session id: ${verification.id}"
        if (status != VeriffStatus.approved) {
            logger.info { "Verification not approved. $message" }
            return null
        }
        return verification.person
            ?: throw VeriffException("Missing verification person data. $message")
    }

    private fun getVeriffDocument(verification: VeriffVerification): VeriffDocument {
        if (verification.document == null) {
            val reason = VeriffReasonCode.fromInt(verification.reasonCode)
            throw VeriffException(
                "Missing document. Reason: ${reason?.code} - ${reason?.name}. Session id: ${verification.id}"
            )
        }
        return verification.document
    }

    internal fun mapVeriffResponse(response: String): VeriffResponse =
        try {
            objectMapper.readValue(response)
        } catch (ex: JsonProcessingException) {
            throw VeriffException("Could not map Veriff response", ex)
        }

    private fun verifyUser(userInfo: UserInfo, vendorData: String?) {
        vendorData?.let {
            try {
                userService.connectUserInfo(it, userInfo.sessionId)
                ServiceUtils.wrapOptional(veriffSessionRepository.findById(userInfo.sessionId))?.let { session ->
                    session.connected = true
                }
            } catch (ex: IllegalArgumentException) {
                logger.warn("Vendor data: $it is not in valid format", ex)
            }
        }
    }
}
