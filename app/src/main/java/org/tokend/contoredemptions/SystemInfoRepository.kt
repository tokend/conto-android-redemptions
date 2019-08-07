package org.tokend.contoredemptions

import io.reactivex.Observable
import io.reactivex.Single
import org.tokend.contoredemptions.base.data.repository.SimpleSingleItemRepository
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.general.model.SystemInfo
import org.tokend.wallet.NetworkParams

class SystemInfoRepository(
    private val apiProvider: ApiProvider
) : SimpleSingleItemRepository<SystemInfo>() {
    override fun getItem(): Observable<SystemInfo> {
        return apiProvider.getApi().general.getSystemInfo().toSingle().toObservable()
    }

    fun getNetworkParams(): Single<NetworkParams> {
        return updateIfNotFreshDeferred()
            .toSingle {
                item?.toNetworkParams()
                    ?: throw IllegalStateException("Missing network passphrase")
            }
    }
}