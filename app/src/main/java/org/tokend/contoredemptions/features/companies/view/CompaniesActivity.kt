package org.tokend.contoredemptions.features.companies.view

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_companies.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseActivity
import org.tokend.contoredemptions.features.companies.data.CompaniesRepository
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord
import org.tokend.contoredemptions.features.companies.view.adapter.CompaniesAdapter
import org.tokend.contoredemptions.features.companies.view.adapter.CompanyListItem
import org.tokend.contoredemptions.util.Navigator
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.contoredemptions.util.SearchUtil
import org.tokend.contoredemptions.view.util.*

class CompaniesActivity : BaseActivity() {

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val companiesRepository: CompaniesRepository
        get() = repositoryProvider.companies()

    private var searchItem: MenuItem? = null

    private lateinit var companiesAdapter: CompaniesAdapter
    private lateinit var layoutManager: GridLayoutManager

    private var filter: String? = null
        set(value) {
            if (value != field) {
                field = value
                onFilterChanged()
            }
        }

    private val canGoBack: Boolean
        get() = intent.getBooleanExtra(CAN_GO_BACK_EXTRA, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_companies)

        initToolbar()
        initSwipeRefresh()
        initCompaniesList()

        subscribeToCompanies()
        update()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        title = getString(R.string.select_company_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(canGoBack)
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }

    private fun initCompaniesList() {
        val columns = ColumnCalculator.getColumnCount(this)

        layoutManager = GridLayoutManager(this, columns)
        companiesAdapter = CompaniesAdapter()

        recycler_view.layoutManager = layoutManager
        recycler_view.adapter = companiesAdapter

        error_empty_view.setEmptyDrawable(R.drawable.ic_briefcase)
        error_empty_view.observeAdapter(companiesAdapter, R.string.error_no_companies)
        error_empty_view.setEmptyViewDenial { companiesRepository.isNeverUpdated }

        companiesAdapter.onItemClick { _, item ->
            item.source?.also(this::onCompanySelected)
        }

        companiesAdapter.registerAdapterDataObserver(
                ScrollOnTopItemUpdateAdapterObserver(recycler_view)
        )

        ElevationUtil.initScrollElevation(recycler_view, appbar_elevation_view)
    }

    private var companiesDisposable: CompositeDisposable? = null

    private fun subscribeToCompanies() {
        companiesDisposable?.dispose()

        companiesRepository.itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    displayCompanies()
                }
                .addTo(compositeDisposable)

        companiesRepository.loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    loadingIndicator.setLoading(it, "companies")
                }
                .addTo(compositeDisposable)

        companiesRepository.errorsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { error ->
                    if (!companiesAdapter.hasData) {
                        error_empty_view.showError(error, errorHandlerFactory.getDefault()) {
                            update(true)
                        }
                    } else {
                        errorHandlerFactory.getDefault().handle(error)
                    }
                }
                .addTo(compositeDisposable)
    }

    private fun onFilterChanged() {
        displayCompanies()
    }

    private fun displayCompanies() {
        val items = companiesRepository.itemsList
                .asSequence()
                .map { company ->
                    CompanyListItem(company)
                }
                .sortedWith(Comparator { o1, o2 ->
                    return@Comparator o1.name.compareTo(o2.name, true)
                })
                .toList()
                .let { items ->
                    filter?.let {
                        items.filter { item ->
                            SearchUtil.isMatchGeneralCondition(it, item.name, item.industry)
                        }
                    } ?: items
                }

        companiesAdapter.setData(items)
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            companiesRepository.updateIfNotFresh()
        } else {
            companiesRepository.update()
        }
    }

    private fun onCompanySelected(company: CompanyRecord) {
        companyProvider.setCompany(company)
        setResult(Activity.RESULT_OK)
        Navigator.from(this).toDashboard()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateListColumnsCount()
    }

    private fun updateListColumnsCount() {
        layoutManager.spanCount = ColumnCalculator.getColumnCount(this)
        companiesAdapter.drawDividers = layoutManager.spanCount == 1
    }

    override fun onBackPressed() {
        if (searchItem?.isActionViewExpanded == true) {
            searchItem?.collapseActionView()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.explore, menu)

        try {
            val searchItem = menu?.findItem(R.id.search)!!

            val searchManager = MenuSearchViewManager(searchItem, toolbar, compositeDisposable)

            searchManager.queryHint = getString(R.string.search)
            searchManager
                .queryChanges
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { newValue ->
                    filter = newValue.takeIf { it.isNotEmpty() }
                }
                .addTo(compositeDisposable)

            this.searchItem = searchItem
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return super.onCreateOptionsMenu(menu)
    }

    companion object {
        private const val CAN_GO_BACK_EXTRA = "can_go_back"

        fun getBundle(canGoBack: Boolean) = Bundle().apply {
            putBoolean(CAN_GO_BACK_EXTRA, canGoBack)
        }
    }
}
