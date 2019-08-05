package org.tokend.contoredemptions.di.repoprovider

import com.fasterxml.jackson.databind.ObjectMapper
import org.tokend.contoredemptions.di.apiprovider.ApiProvider

class AppRepositoryProvider(
    private val apiProvider: ApiProvider,
    private val objectMapper: ObjectMapper
): RepositoryProvider {
}