package org.tokend.contoredemptions.features.identity.storage

import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.features.identity.model.IdentityRecord
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.identity.params.IdentitiesPageParams
import retrofit2.HttpException
import java.net.HttpURLConnection

class AccountDetailsRepository(
        private val apiProvider: ApiProvider
) {
    class NoIdentityAvailableException : Exception()

    private val identities = mutableSetOf<IdentityRecord>()

    /**
     * Loads account ID for given identifier.
     * Result will be cached.
     *
     * @param identifier - email or phone number
     *
     * @see NoIdentityAvailableException
     */
    fun getAccountIdByIdentifier(identifier: String): Single<String> {
        val formattedIdentifier = identifier.toLowerCase()
        val existing = identities.find {
            it.email == formattedIdentifier || it.phoneNumber == formattedIdentifier
        }?.accountId
        if (existing != null) {
            return Single.just(existing)
        }

        return getIdentity(IdentitiesPageParams(identifier = formattedIdentifier))
                .map(IdentityRecord::accountId)
    }

    /**
     * Loads email for given account ID.
     * Result will be cached.
     *
     * @see NoIdentityAvailableException
     */
    fun getEmailByAccountId(accountId: String): Single<String> {
        val existing = identities.find { it.accountId == accountId }?.email
        if (existing != null) {
            return Single.just(existing)
        }

        return getIdentity(IdentitiesPageParams(address = accountId))
                .map(IdentityRecord::email)
    }

    /**
     * Loads phone number for given account ID if it exists.
     * Result will be cached.
     *
     * @see NoIdentityAvailableException
     */
    fun getPhoneByAccountId(accountId: String): Maybe<String> {
        val existingIdentity = identities.find { it.accountId == accountId }

        if (existingIdentity != null) {
            return existingIdentity.phoneNumber.toMaybe()
        }

        return getIdentity(IdentitiesPageParams(address = accountId))
                .flatMapMaybe { it.phoneNumber.toMaybe() }
    }

    private fun getIdentity(params: IdentitiesPageParams): Single<IdentityRecord> {
        return apiProvider
                .getApi()
                .identities
                .get(params)
                .toSingle()
                .map { detailsPage ->
                    detailsPage.items.firstOrNull()
                            ?: throw NoIdentityAvailableException()
                }
                .onErrorResumeNext {
                    if (it is HttpException && it.code() == HttpURLConnection.HTTP_NOT_FOUND)
                        Single.error(NoIdentityAvailableException())
                    else
                        Single.error(it)
                }
                .map(::IdentityRecord)
                .doOnSuccess { identities.add(it) }
    }

    fun getCachedIdentity(accountId: String): IdentityRecord? {
        return identities.find { it.accountId == accountId }
    }

    fun invalidateCachedIdentity(accountId: String) {
        identities.remove(getCachedIdentity(accountId))
    }
}