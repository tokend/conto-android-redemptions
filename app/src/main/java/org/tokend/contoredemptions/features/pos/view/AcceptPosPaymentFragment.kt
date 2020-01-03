package org.tokend.contoredemptions.features.pos.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_accept_pos_payment.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseFragment
import org.tokend.contoredemptions.features.assets.data.model.Asset
import org.tokend.contoredemptions.features.assets.data.storage.AssetsRepository
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.contoredemptions.view.util.LoadingIndicatorManager

class AcceptPosPaymentFragment : BaseFragment() {
    private val assetsRepository: AssetsRepository
        get() = repositoryProvider.assets()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private var asset: Asset? = null
        set(value) {
            field = value
            onAssetChanged()
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_accept_pos_payment, container, false)
    }

    override fun onInitAllowed() {
        initSwipeRefresh()

        subscribeToAssets()

        update()
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.colorAccent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }

    private fun subscribeToAssets() {
        assetsRepository.loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { isLoading ->
                    loadingIndicator.setLoading(isLoading)
                    if (assetsRepository.isNeverUpdated) {
                        asset_name_text_view.text = getString(R.string.loading_data)
                    }
                }
                .addTo(compositeDisposable)

        assetsRepository.itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { onAssetsUpdated() }
                .addTo(compositeDisposable)

        assetsRepository.errorsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { error ->
                    val errorHandler = errorHandlerFactory.getDefault()
                    if (assetsRepository.isNeverUpdated) {
                        error_empty_view.showError(error, errorHandler) {
                            update(force = true)
                        }
                    } else {
                        errorHandler.handle(error)
                    }
                }
                .addTo(compositeDisposable)
    }

    private fun onAssetsUpdated() {
        displayAssets()
    }

    private fun displayAssets() {
        val company = companyProvider.getCompany()

        val assets = assetsRepository.itemsList
                .filter { it.ownerAccountId == company.id }

        if (asset == null) {
            val firstAsset = assets.firstOrNull()
            if (firstAsset != null) {
                asset = firstAsset
            } else {
                asset_name_text_view.text = getString(R.string.error_no_assets)
            }
        }
    }

    private fun onAssetChanged() {
        asset_name_text_view.text = asset?.name ?: asset?.code
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            assetsRepository.updateIfNotFresh()
        } else {
            assetsRepository.update()
        }
    }

    companion object {
        fun newInstance() = AcceptPosPaymentFragment()
    }
}