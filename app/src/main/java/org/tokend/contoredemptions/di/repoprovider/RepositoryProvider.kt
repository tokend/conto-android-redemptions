package org.tokend.contoredemptions.di.repoprovider

import org.tokend.contoredemptions.SystemInfoRepository
import org.tokend.contoredemptions.features.companies.data.CompaniesRepository
import org.tokend.contoredemptions.features.history.data.storage.RedemptionsRepository

interface RepositoryProvider {
    fun systemInfo(): SystemInfoRepository
    fun companies(): CompaniesRepository
    fun redemptions(companyId: String): RedemptionsRepository
}