package org.tokend.contoredemptions.di.repoprovider

import org.tokend.contoredemptions.features.companies.data.CompaniesRepository

interface RepositoryProvider {
    fun companies(): CompaniesRepository
}