package org.tokend.contoredemptions.di.companyprovider

import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord

interface CompanyProvider {
    fun hasCompany(): Boolean
    fun getCompany(): CompanyRecord
    fun setCompany(company: CompanyRecord)
    val lastCompanyId: String?
}