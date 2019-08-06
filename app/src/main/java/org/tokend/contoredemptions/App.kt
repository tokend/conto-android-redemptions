package org.tokend.contoredemptions

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
import org.tokend.contoredemptions.di.AppComponent
import org.tokend.contoredemptions.di.DaggerAppComponent
import org.tokend.contoredemptions.di.UtilsModule
import org.tokend.contoredemptions.di.apiprovider.ApiProviderModule
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProviderModule
import org.tokend.contoredemptions.util.UrlConfig
import java.io.IOException
import java.net.SocketException

class App : MultiDexApplication() {
    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        initDi()
        initTls()
        initRxErrorHandler()
        initPicasso()
    }

    private fun initDi() {
        appComponent = DaggerAppComponent
            .builder()
            .utilsModule(
                UtilsModule(this)
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
                    8L * 1024 * 1024
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
}