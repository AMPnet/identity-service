package com.ampnet.identityservice.persistence.model

import javax.persistence.Embeddable

@Embeddable
class Document(
    var type: String,
    var country: String,
    var number: String?,
    var validUntil: String?,
    var validFrom: String?
) {
    constructor(veriffDocument: VeriffDocument) : this(
        veriffDocument.type,
        veriffDocument.country,
        veriffDocument.number,
        veriffDocument.validUntil,
        veriffDocument.validFrom
    )
}

// TODO place in right dir
data class VeriffDocument(
    val type: String,
    val country: String,
    val number: String?,
    val validUntil: String?,
    val validFrom: String?
)
