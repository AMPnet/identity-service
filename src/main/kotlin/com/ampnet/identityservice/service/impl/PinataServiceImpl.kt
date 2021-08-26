package com.ampnet.identityservice.service.impl

import com.ampnet.identityservice.config.ApplicationProperties
import com.ampnet.identityservice.exception.ErrorCode
import com.ampnet.identityservice.exception.InternalException
import com.ampnet.identityservice.service.PinataService
import com.fasterxml.jackson.annotation.JsonProperty
import mu.KLogging
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

@Service
class PinataServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val restTemplate: RestTemplate
) : PinataService {

    companion object : KLogging()

    private val pinataUrl = "https://api.pinata.cloud/users/generateApiKey"

    @Throws(InternalException::class)
    override fun getUserJwt(address: String): PinataResponse {
        val request = PinataApiKeyRequest(address, maxUses = applicationProperties.pinata.maxUses)
        try {
            val httpEntity = HttpEntity(request, generateHeaders())
            val response = restTemplate.postForEntity<PinataResponse>(pinataUrl, httpEntity)
            if (response.statusCode.is2xxSuccessful) {
                return response.body ?: throw InternalException(
                    ErrorCode.USER_PINATA_JWT,
                    "Missing body for successful response"
                )
            } else {
                throw InternalException(
                    ErrorCode.USER_PINATA_JWT,
                    "Unsuccessful request to get Pinata JWT: ${response.statusCode}"
                )
            }
        } catch (ex: RestClientException) {
            throw InternalException(ErrorCode.USER_PINATA_JWT, "Failed to get Pinata JWT")
        }
    }

    private fun generateHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer ${applicationProperties.pinata.jwt}")
        return headers
    }

    private data class PinataApiKeyRequest(
        val keyName: String,
        val maxUses: Int = 10,
        val permissions: PinataPermissions = PinataPermissions()
    )

    private data class PinataPermissions(val endpoints: PinataEndpoints = PinataEndpoints())
    private data class PinataEndpoints(val pinning: PinataPinning = PinataPinning())
    private data class PinataPinning(
        val pinFileToIPFS: Boolean = true,
        val pinByHash: Boolean = true,
        val pinJSONToIPFS: Boolean = true,
        val pinJobs: Boolean = true
    )
}

data class PinataResponse(
    @JsonProperty("pinata_api_key")
    val pinataApiKey: String,
    @JsonProperty("pinata_api_secret")
    val pinataApiSecret: String,
    @JsonProperty("JWT")
    val jwt: String
)
