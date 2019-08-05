package org.tokend.contoredemptions.base.view

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.reactivex.disposables.CompositeDisposable
import org.tokend.contoredemptions.App
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.di.repoprovider.RepositoryProvider
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProvider
import org.tokend.contoredemptions.util.errorhandler.ErrorHandlerFactory
import org.tokend.contoredemptions.util.formatter.AmountFormatter
import org.tokend.contoredemptions.view.ToastManager
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity() {
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

    /**
     * Disposable holder which will be disposed on activity destroy
     */
    protected val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.background)))
        super.onCreate(savedInstanceState)

        (application as App).appComponent.inject(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val enterTransition = window?.enterTransition
            if (enterTransition != null) {
                enterTransition.excludeTarget(android.R.id.statusBarBackground, true)
                enterTransition.excludeTarget(android.R.id.navigationBarBackground, true)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}