package org.tokend.contoredemptions.di

import dagger.Component
import org.tokend.contoredemptions.base.view.BaseActivity
import org.tokend.contoredemptions.base.view.BaseFragment
import org.tokend.contoredemptions.di.apiprovider.ApiProviderModule
import org.tokend.contoredemptions.di.companyprovider.CompanyProviderModule
import org.tokend.contoredemptions.di.repoprovider.RepositoryProviderModule
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProviderModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ApiProviderModule::class,
        UrlConfigProviderModule::class,
        RepositoryProviderModule::class,
        CompanyProviderModule::class,
        AppDatabaseModule::class,
        LocaleManagerModule::class,
        UtilsModule::class,
        PersistenceModule::class
    ]
)
interface AppComponent {
    fun inject(baseActivity: BaseActivity)
    fun inject(baseFragment: BaseFragment)
}