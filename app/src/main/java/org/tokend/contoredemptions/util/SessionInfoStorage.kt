package org.tokend.contoredemptions.util

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord

class SessionInfoStorage(
    private val sharedPreferences: SharedPreferences
) {

    fun saveLastCompany(company: CompanyRecord) {
        val c = Gson().toJson(company)
        sharedPreferences.edit()
            .putString(LAST_COMPANY_KEY, c)
            .apply()
    }

    fun loadLastCompany(): CompanyRecord? {
        return try {
            Gson().fromJson(
                sharedPreferences
                    .getString(LAST_COMPANY_KEY, null), CompanyRecord::class.java
            )
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    private companion object {
        private const val LAST_COMPANY_KEY = "last_company"
    }
}