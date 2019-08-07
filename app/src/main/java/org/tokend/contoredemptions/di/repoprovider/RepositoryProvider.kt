package org.tokend.contoredemptions.di.repoprovider

import org.tokend.contoredemptions.SystemInfoRepository
import org.tokend.contoredemptions.features.companies.data.CompaniesRepository

interface RepositoryProvider {
    fun systemInfo(): SystemInfoRepository
    fun companies(): CompaniesRepository
}