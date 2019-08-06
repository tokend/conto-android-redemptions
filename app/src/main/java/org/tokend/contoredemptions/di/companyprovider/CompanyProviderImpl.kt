package org.tokend.contoredemptions.di.companyprovider

import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord

class CompanyProviderImpl : CompanyProvider {
    private var company: CompanyRecord? = null

    override fun hasCompany(): Boolean {
        return company != null
    }

    override fun getCompany(): CompanyRecord {
        return company!!
    }

    override fun setCompany(company: CompanyRecord) {
        this.company = company
    }
}