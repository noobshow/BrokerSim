package giuliolodi.financegame.ui.main

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast
import giuliolodi.financegame.R
import giuliolodi.financegame.models.StockDb
import giuliolodi.financegame.ui.base.BaseActivity
import giuliolodi.financegame.ui.fragment.Fragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.main_activity_content.*
import javax.inject.Inject

class MainActivity : BaseActivity(), MainContract.View {

    @Inject lateinit var mPresenter: MainContract.Presenter<MainContract.View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        initLayout()

        getActivityComponent().inject(this)

        mPresenter.onAttach(this)

        mPresenter.subscribe()
    }

    private fun initLayout() {
        setSupportActionBar(main_activity_toolbar)
        val adapter: MainAdapter = MainAdapter()

        main_activity_fab.setOnClickListener { mPresenter.addMoney() }

        main_activity_content_rv.layoutManager = LinearLayoutManager(applicationContext)
        main_activity_content_rv.adapter = adapter

        main_activity_content_srl.setColorScheme(R.color.colorAccent)
        main_activity_content_srl.setOnRefreshListener { mPresenter.subscribe() }

        adapter.getPositionClicks()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { showFragment() }
    }

    override fun showLoading() {
        main_activity_content_srl.isRefreshing = true
    }

    override fun hideLoading() {
        main_activity_content_srl.isRefreshing = false
    }

    override fun showFragment() {
        val fragment: Fragment = Fragment()
        fragment.show(supportFragmentManager, "Stock Fragment")
    }

    override fun showContent(stocks: List<StockDb>) {
        (main_activity_content_rv.adapter as MainAdapter).addStocks(stocks)
    }

    override fun showError(error: String) {
        Snackbar.make(currentFocus, "Error retrieving data", Snackbar.LENGTH_LONG).show()
    }

    override fun updateMoney(money: String) {
        main_activity_toolbar.title = money
    }

    override fun onDestroy() {
        mPresenter.onDetach()
        super.onDestroy()
    }

}
