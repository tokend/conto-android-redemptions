package org.tokend.contoredemptions.features.history.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_redemptions.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseFragment
import org.tokend.contoredemptions.features.history.data.model.RedemptionRecord
import org.tokend.contoredemptions.features.history.data.storage.RedemptionsRepository
import org.tokend.contoredemptions.features.history.view.adapter.RedemptionListItem
import org.tokend.contoredemptions.features.history.view.adapter.RedemptionsAdapter
import org.tokend.contoredemptions.util.Navigator
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.contoredemptions.util.formatter.AccountIdFormatter
import org.tokend.contoredemptions.view.util.ElevationUtil

class RedemptionsFragment : BaseFragment() {
    private val redemptionsRepository: RedemptionsRepository
        get() = repositoryProvider.redemptions(companyProvider.getCompany().id)

    private val accountIdFormatter = AccountIdFormatter()

    private lateinit var adapter: RedemptionsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_redemptions, container, false)
    }

    override fun onInitAllowed() {
        initList()

        subscibeToRedemptions()

        update()
    }

    private fun initList() {
        adapter = RedemptionsAdapter(amountFormatter, dateFormatter)
        adapter.onItemClick { _, item ->
            item.source?.also(this::openDetails)
        }

        history_list.adapter = adapter
        history_list.layoutManager = LinearLayoutManager(requireContext())
        history_list.listenBottomReach({ adapter.getDataItemCount() }) {
            redemptionsRepository.loadMore() || redemptionsRepository.noMoreItems
        }

        error_empty_view.observeAdapter(adapter, R.string.no_redemptions_yet)
        error_empty_view.setEmptyViewDenial { redemptionsRepository.isNeverUpdated }

        date_text_switcher.init(history_list, adapter)

        ElevationUtil.initScrollElevation(history_list, appbar_elevation_view)
    }

    private fun subscibeToRedemptions() {
        redemptionsRepository
                .itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { displayRedemptions() }
                .addTo(compositeDisposable)

        redemptionsRepository
                .loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { isLoading ->
                    if (isLoading) {
                        adapter.showLoadingFooter()
                    } else {
                        adapter.hideLoadingFooter()
                    }
                }
                .addTo(compositeDisposable)

        redemptionsRepository
                .errorsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { error ->
                    if (!adapter.hasData) {
                        error_empty_view.showError(error, errorHandlerFactory.getDefault())
                    } else {
                        errorHandlerFactory.getDefault().handle(error)
                    }
                }
                .addTo(compositeDisposable)
    }

    private fun displayRedemptions() {
        val items = redemptionsRepository.itemsList
                .map { RedemptionListItem(it, accountIdFormatter) }
        adapter.setData(items)
    }

    private fun openDetails(item: RedemptionRecord) {
        Navigator.from(this).openRedemptionDetails(item)
    }

    private fun update() {
        redemptionsRepository.updateIfNotFresh()
    }
}