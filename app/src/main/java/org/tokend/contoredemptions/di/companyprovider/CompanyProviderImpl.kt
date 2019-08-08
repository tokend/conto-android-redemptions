package org.tokend.contoredemptions.di.companyprovider

import org.tokend.contoredemptions.SessionInfoStorage
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord

class CompanyProviderImpl(
    private val sessionInfoStorage: SessionInfoStorage? = null
) : CompanyProvider {
    private var company: CompanyRecord? = null

    override fun hasCompany(): Boolean {
        return company != null
    }

    override fun getCompany(): CompanyRecord {
        return company!!
    }

    override fun setCompany(company: CompanyRecord) {
        this.company = company
        sessionInfoStorage?.saveLastCompanyId(company.id)
    }

    override val lastCompanyId: String?
        get() = sessionInfoStorage?.loadLastCompanyId()
}