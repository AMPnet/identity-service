package com.ampnet.identityservice.persistence.model

import com.ampnet.identityservice.service.pojo.VeriffDocument
import javax.persistence.Embeddable

@Embeddable
class Document(
    var type: String?,
    var country: String?,
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
