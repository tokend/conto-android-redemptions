package org.tokend.contoredemptions.di.repoprovider

import org.tokend.contoredemptions.features.assets.data.storage.AssetsRepository
import org.tokend.contoredemptions.features.companies.data.CompaniesRepository
import org.tokend.contoredemptions.features.history.data.storage.RedemptionsRepository
import org.tokend.contoredemptions.features.identity.storage.AccountDetailsRepository
import org.tokend.contoredemptions.features.transactions.storage.SystemInfoRepository

interface RepositoryProvider {
    fun systemInfo(): SystemInfoRepository
    fun companies(): CompaniesRepository
    fun redemptions(companyId: String): RedemptionsRepository
    fun assets(): AssetsRepository
    fun accountDetails(): AccountDetailsRepository
}