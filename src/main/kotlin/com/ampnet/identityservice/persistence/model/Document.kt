package com.ampnet.identityservice.persistence.model

import com.ampnet.identityservice.service.pojo.VeriffDocument
import javax.persistence.Embeddable

@Embeddable
class Document(
    // type: String(one of PASSPORT, ID_CARD, DRIVERS_LICENSE, RESIDENCE_PERMIT, OTHER) Document type
    var type: String?,
    // country: ISO-2 String Document country
    var country: String?,
    // number: String Document number
    var number: String?,
    // validUntil: String Document is valid until date in YYYY-MM-DD format
    var validUntil: String?,
    // validFrom: String Document is valid from date in YYYY-MM-DD format
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
