package org.tokend.contoredemptions.features.companies.view.adapter

import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord

class CompanyListItem(
    val id: String,
    val name: String,
    val industry: String?,
    val logoUrl: String?,
    val source: CompanyRecord?
) {
    constructor(source: CompanyRecord) : this(
        id = source.id,
        name = source.name,
        industry = source.industry,
        logoUrl = source.logoUrl,
        source = source
    )
}