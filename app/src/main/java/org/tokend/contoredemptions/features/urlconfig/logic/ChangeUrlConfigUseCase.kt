package org.tokend.contoredemptions.features.urlconfig.logic

import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.HttpUrl
import org.tokend.contoredemptions.di.companyprovider.CompanyProvider
import org.tokend.contoredemptions.features.companies.data.CompaniesRepository
import org.tokend.contoredemptions.features.urlconfig.model.UrlConfig
import org.tokend.sdk.api.custom.CustomRequestsService
import org.tokend.sdk.factory.GsonFactory
import org.tokend.sdk.factory.ServiceFactory
import java.net.HttpURLConnection

/**
 * Loads web client's 'env.js' file, creates [UrlConfig] from it
 * and applies via [urlConfigManager]. Clears [companyProvider] and [companiesRepository]
 * if they are present
 */
class ChangeUrlConfigUseCase(
        private val webClientUrl: String,
        private val urlConfigManager: UrlConfigManager,
        private val companiesRepository: CompaniesRepository?,
        private val companyProvider: CompanyProvider?
) {
    class InvalidNetworkException : Exception()

    private lateinit var urlConfig: UrlConfig

    fun perform(): Completable {
        return getUrlConfig()
                .doOnSuccess { urlConfig ->
                    this.urlConfig = urlConfig
                }
                .flatMap {
                    applyNewConfig()
                }
                .doOnSuccess {
                    updateRepositories()
                }
                .ignoreElement()
    }

    private fun getUrlConfig(): Single<UrlConfig> = Single.defer {
        val webClientUrl = UrlConfig(
                client = webClientUrl,
                api = "",
                storage = ""
        ).client

        val parsedClientUrl = HttpUrl.parse(webClientUrl)
                ?: throw InvalidNetworkException()

        val envJsResponse = ServiceFactory(
                url = parsedClientUrl.toString(),
                withLogs = false,
                userAgent = null
        )
                .getCustomService(CustomRequestsService::class.java)
                .get(
                        url = parsedClientUrl.newBuilder()
                                .addEncodedPathSegments("/static/env.js")
                                .build()
                                .toString(),
                        headers = emptyMap(),
                        query = emptyMap()
                )
                .execute()

        if (!envJsResponse.isSuccessful
                && envJsResponse.code() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw InvalidNetworkException()
        }

        val envJsString = envJsResponse
                .body()
                .string()

        val envJson = envJsString
                .trimIndent()
                .replace("""document.ENV\s*=\s*""".toRegex(), "")
                .let { GsonFactory().getBaseGson().fromJson(it, JsonObject::class.java) }

        val urlConfig = try {
            UrlConfig(
                    api = envJson["HORIZON_SERVER"].asString,
                    storage = envJson["FILE_STORAGE"].asString,
                    client = parsedClientUrl.toString()
            )
        } catch (_: Exception) {
            throw InvalidNetworkException()
        }

        Single.just(urlConfig)
    }.subscribeOn(Schedulers.newThread())

    private fun applyNewConfig(): Single<Boolean> {
        urlConfigManager.set(urlConfig)
        return Single.just(true)
    }

    private fun updateRepositories() {
        companyProvider?.clear()
        companiesRepository?.clear()
    }
}