package org.tokend.contoredemptions.base.view

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import org.tokend.contoredemptions.App
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.di.companyprovider.CompanyProvider
import org.tokend.contoredemptions.di.repoprovider.RepositoryProvider
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProvider
import org.tokend.contoredemptions.util.errorhandler.ErrorHandlerFactory
import org.tokend.contoredemptions.util.errorhandler.ErrorLogger
import org.tokend.contoredemptions.util.formatter.AmountFormatter
import org.tokend.contoredemptions.util.formatter.DateFormatter
import org.tokend.contoredemptions.view.ToastManager
import javax.inject.Inject

abstract class BaseFragment : Fragment() {
    @Inject
    lateinit var toastManager: ToastManager
    @Inject
    lateinit var errorHandlerFactory: ErrorHandlerFactory
    @Inject
    lateinit var repositoryProvider: RepositoryProvider
    @Inject
    lateinit var urlConfigProvider: UrlConfigProvider
    @Inject
    lateinit var amountFormatter: AmountFormatter
    @Inject
    lateinit var companyProvider: CompanyProvider
    @Inject
    lateinit var apiProvider: ApiProvider
    @Inject
    lateinit var dateFormatter: DateFormatter
    @Inject
    lateinit var errorLogger: ErrorLogger

    /**
     * Disposable holder which will be disposed on fragment destroy
     */
    protected lateinit var compositeDisposable: CompositeDisposable

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as App).appComponent.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        compositeDisposable = CompositeDisposable()
        if (savedInstanceState == null) {
            onInitAllowed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.dispose()
    }

    /**
     * You must implement your fragment initialization here
     */
    abstract fun onInitAllowed()
}