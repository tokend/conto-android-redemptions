package org.tokend.contoredemptions

import android.content.SharedPreferences

class SessionInfoStorage(
    private val sharedPreferences: SharedPreferences
) {

    fun saveLastCompanyId(companyId: String) {
        sharedPreferences.edit()
            .putString(LAST_COMPANY_KEY, companyId)
            .apply()
    }

    fun loadLastCompanyId(): String? {
        return sharedPreferences
            .getString(LAST_COMPANY_KEY, null)
    }

    private companion object {
        private const val LAST_COMPANY_KEY = "last_company"
    }
}