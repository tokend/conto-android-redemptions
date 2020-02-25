package org.tokend.contoredemptions.di.companyprovider

import org.tokend.contoredemptions.base.data.repository.ObjectPersistence
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord

class CompanyProviderImpl(
    private val lastCompanyPersistence: ObjectPersistence<CompanyRecord>? = null
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
        lastCompanyPersistence?.saveItem(company)
    }

    override val lastCompany: CompanyRecord?
        get() = lastCompanyPersistence?.loadItem()

    override fun clear() {
        this.company = null
        lastCompanyPersistence?.clear()
    }
}