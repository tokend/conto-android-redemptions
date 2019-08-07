package org.tokend.contoredemptions.di

import dagger.Module
import dagger.Provides
import org.tokend.contoredemptions.util.locale.AppLocaleManager
import javax.inject.Singleton

@Module
class LocaleManagerModule(
        private val localeManager: AppLocaleManager
) {
    @Provides
    @Singleton
    fun localeManager() = localeManager
}