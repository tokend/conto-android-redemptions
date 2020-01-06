package org.tokend.contoredemptions.features.balances.data.storage

import io.reactivex.Single
import org.tokend.contoredemptions.base.data.repository.RepositoryCache
import org.tokend.contoredemptions.base.data.repository.SimpleMultipleItemsRepository
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.features.balances.data.model.BalanceRecord
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.generated.resources.AccountResource
import org.tokend.sdk.api.v3.accounts.params.AccountParamsV3

class BalancesRepository(
        private val accountId: String,
        private val apiProvider: ApiProvider,
        itemsCache: RepositoryCache<BalanceRecord>
) : SimpleMultipleItemsRepository<BalanceRecord>(itemsCache) {
    override fun getItems(): Single<List<BalanceRecord>> {
        return apiProvider.getApi().v3.accounts
                .getById(
                        accountId = accountId,
                        params = AccountParamsV3(listOf(
                                AccountParamsV3.Includes.BALANCES,
                                AccountParamsV3.Includes.BALANCES_ASSET
                        ))
                )
                .toSingle()
                .map(AccountResource::getBalances)
                .map { balances ->
                    balances.map(::BalanceRecord)
                }
    }
}