package org.tokend.contoredemptions.features.urlconfig.view

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_change_network.*
import kotlinx.android.synthetic.main.progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseActivity
import org.tokend.contoredemptions.features.urlconfig.logic.ChangeUrlConfigUseCase
import org.tokend.contoredemptions.features.urlconfig.logic.UrlConfigManager
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.contoredemptions.view.util.LoadingIndicatorManager
import org.tokend.contoredemptions.view.util.input.SimpleTextWatcher

class ChangeNetworkActivity : BaseActivity() {
    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )

    private var canConfirm: Boolean = false
        set(value) {
            field = value
            confirm_button.isEnabled = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_network)

        initToolbar()
        initFields()
        initButtons()

        updateConfirmationAvailability()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.change_app_network)
    }

    private fun initFields() {
        url_edit_text.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                url_edit_text_layout.error = null
                updateConfirmationAvailability()
            }
        })

        url_edit_text.setOnEditorActionListener { _, _, _ ->
            tryToConfirm()
            true
        }
    }

    private fun initButtons() {
        confirm_button.setOnClickListener {
            tryToConfirm()
        }
    }

    private fun updateConfirmationAvailability() {
        canConfirm = !url_edit_text.text.isNullOrBlank()
                && url_edit_text_layout.error == null
                && !loadingIndicator.isLoading
    }

    private fun tryToConfirm() {
        if (canConfirm) {
            confirm()
        }
    }

    private var disposable: Disposable? = null
    private fun confirm() {
        val url = url_edit_text.text
                ?.toString()
                ?.trim()
                ?: ""

        disposable?.dispose()
        disposable = ChangeUrlConfigUseCase(
                webClientUrl = url,
                urlConfigManager = UrlConfigManager(urlConfigProvider, urlConfigPersistence),
                companyProvider = companyProvider,
                companiesRepository = repositoryProvider.companies()
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    loadingIndicator.show()
                    updateConfirmationAvailability()
                }
                .doOnTerminate {
                    loadingIndicator.hide()
                    updateConfirmationAvailability()
                }
                .subscribeBy(
                        onComplete = this::onNetworkChanged,
                        onError = this::onNetworkChangeError
                )
                .addTo(compositeDisposable)
    }

    private fun onNetworkChanged() {
        toastManager.short(R.string.network_has_been_changed)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun onNetworkChangeError(error: Throwable) {
        when (error) {
            is ChangeUrlConfigUseCase.InvalidNetworkException ->
                url_edit_text_layout.error = getString(R.string.error_invalid_network)
            else ->
                errorHandlerFactory.getDefault().handle(error)
        }

        updateConfirmationAvailability()
    }

    override fun onBackPressed() {
        finish()
    }
}