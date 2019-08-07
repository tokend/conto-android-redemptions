package org.tokend.contoredemptions.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.tokend.contoredemptions.util.errorhandler.DefaultErrorLogger
import org.tokend.contoredemptions.util.errorhandler.ErrorHandlerFactory
import org.tokend.contoredemptions.util.errorhandler.ErrorLogger
import org.tokend.contoredemptions.util.formatter.AmountFormatter
import org.tokend.contoredemptions.util.formatter.DateFormatter
import org.tokend.contoredemptions.util.formatter.DefaultAmountFormatter
import org.tokend.contoredemptions.util.locale.AppLocaleManager
import org.tokend.contoredemptions.view.ToastManager
import javax.inject.Singleton

@Module
class UtilsModule(private val context: Context) {
    @Provides
    fun context(localeManager: AppLocaleManager): Context {
        return localeManager.getLocalizeContext(context)
    }

    @Provides
    fun toastManager(context: Context): ToastManager {
        return ToastManager(context)
    }

    @Provides
    fun errorHandlerFactory(context: Context,
                            toastManager: ToastManager,
                            errorLogger: ErrorLogger): ErrorHandlerFactory {
        return ErrorHandlerFactory(context, toastManager, errorLogger)
    }

    @Provides
    @Singleton
    fun amountFormatter(): AmountFormatter = DefaultAmountFormatter()

    @Provides
    @Singleton
    fun errorLogger(): ErrorLogger {
        return DefaultErrorLogger()
    }

    @Provides
    fun dateFormatter(context: Context): DateFormatter {
        return DateFormatter(context)
    }
}