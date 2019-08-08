package org.tokend.contoredemptions

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.jakewharton.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import okhttp3.CookieJar
import org.jetbrains.anko.defaultSharedPreferences
import org.tokend.contoredemptions.di.*
import org.tokend.contoredemptions.di.apiprovider.ApiProviderModule
import org.tokend.contoredemptions.di.companyprovider.CompanyProviderImpl
import org.tokend.contoredemptions.di.companyprovider.CompanyProviderModule
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProviderModule
import org.tokend.contoredemptions.util.SessionInfoStorage
import org.tokend.contoredemptions.util.UrlConfig
import org.tokend.contoredemptions.util.locale.AppLocaleManager
import java.io.IOException
import java.net.SocketException

class App : MultiDexApplication() {
    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        initLocale()
        initDi()
        initTls()
        initRxErrorHandler()
        initPicasso()
    }

    private fun initLocale() {
        mLocaleManager = AppLocaleManager(this, getAppPreferences())
        localeManager.initLocale()
    }

    private fun initDi() {
        appComponent = DaggerAppComponent
                .builder()
                .utilsModule(
                        UtilsModule(localeManager.getLocalizeContext(this))
                )
                .urlConfigProviderModule(
                        UrlConfigProviderModule(
                                UrlConfig(
                                        BuildConfig.API_URL,
                                        BuildConfig.STORAGE_URL,
                                        BuildConfig.CLIENT_URL
                                )
                        )
                )
                .apiProviderModule(
                        ApiProviderModule(CookieJar.NO_COOKIES)
                )
                .appDatabaseModule(
                        AppDatabaseModule(DATABASE_NAME)
                )
                .localeManagerModule(LocaleManagerModule(localeManager))
                .companyProviderModule(
                    CompanyProviderModule(
                        CompanyProviderImpl(
                            SessionInfoStorage(defaultSharedPreferences)
                        )
                    )
                )
                .build()
    }

    private fun initTls() {
        try {
            if (areGooglePlayServicesAvailable()) {
                ProviderInstaller.installIfNeeded(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initRxErrorHandler() {
        RxJavaPlugins.setErrorHandler {
            var e = it
            if (e is UndeliverableException) {
                e = e.cause
            }
            if ((e is IOException) || (e is SocketException)) {
                // fine, irrelevant network problem or API that throws on cancellation
                return@setErrorHandler
            }
            if (e is InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return@setErrorHandler
            }
            if ((e is NullPointerException) || (e is IllegalArgumentException)) {
                // that's likely a bug in the application
                Thread.currentThread().uncaughtExceptionHandler
                        .uncaughtException(Thread.currentThread(), e)
                return@setErrorHandler
            }
            if (e is IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().uncaughtExceptionHandler
                        .uncaughtException(Thread.currentThread(), e)
                return@setErrorHandler
            }
            Log.w("RxErrorHandler", "Undeliverable exception received", e)
        }
    }

    private fun initPicasso() {
        val picasso = Picasso.Builder(this)
                .downloader(
                        OkHttp3Downloader(
                                cacheDir,
                                IMAGE_CACHE_SIZE_MB * 1024L * 1024
                        )
                )
                .build()
        Picasso.setSingletonInstance(picasso)
    }

    private fun areGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }

    private fun getAppPreferences(): SharedPreferences {
        return getSharedPreferences("App", Context.MODE_PRIVATE)
    }

    companion object {
        private const val DATABASE_NAME = "data"
        private const val IMAGE_CACHE_SIZE_MB = 8

        private lateinit var mLocaleManager: AppLocaleManager
        val localeManager: AppLocaleManager
            get() = mLocaleManager
    }
}