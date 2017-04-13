package giuliolodi.financegame.ui.assets

import android.util.Log
import giuliolodi.financegame.data.DataManager
import giuliolodi.financegame.models.StockDb
import giuliolodi.financegame.models.StockDbBought
import giuliolodi.financegame.ui.base.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import yahoofinance.Stock
import java.util.*
import javax.inject.Inject

class AssetsPresenter<V: AssetsContract.View> : BasePresenter<V>, AssetsContract.Presenter<V> {

    val TAG = "AssetsPresenter"

    @Inject
    constructor(mCompositeDisposable: CompositeDisposable, mDataManager: DataManager): super(mCompositeDisposable, mDataManager)

    /**
     * Retrieve boughtStocks from Realm.
     */
    override fun subscribe() {
        getView().showLoading()
        var storredStocksBought: List<StockDbBought> = ArrayList()
        var storredStocksStrings: ArrayList<String> = ArrayList()
        getCompositeDisposable().add(getDataManager().getBoughtStocks()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete { downloadStocks(storredStocksStrings.toTypedArray(), storredStocksBought) }
                .subscribe(
                        { stocksDb -> storredStocksBought = stocksDb; for (s in stocksDb) storredStocksStrings.add(s.symbol) },
                        { throwable -> Log.e(TAG, throwable.message, throwable) }))
    }

    /**
     * Downloads new data of stored StockDbBoughts through their symbols.
     */
    fun downloadStocks(storredStocksStrings: Array<String>, storredStocks: List<StockDbBought>) {
        var downloadedStocks: List<Stock>? = null
        getCompositeDisposable().add(getDataManager().downloadStockList(storredStocksStrings)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete { checkStocks(storredStocks, downloadedStocks!!) }
                .subscribe(
                        { mappedList -> downloadedStocks = mappedList!!.values.toList() },
                        { throwable ->
                            Log.e(TAG, throwable.message, throwable)
                            getView().hideLoading()
                            getView().showError("Error downloading data. Showing storred stocks")
                            getView().showContent(storredStocks)
                        }))
    }

    /**
     * Check and update stored StockDbBoughts with updated stocks
     */
    fun checkStocks(storredStocks: List<StockDbBought>, downloadedStocks: List<Stock>) {
        getDataManager().updateStockDbBought(downloadedStocks, storredStocks)
        getStocksUpdateView()
    }

    /**
     * Get StockDbBought and show them to the user
     */
    fun getStocksUpdateView() {
        getCompositeDisposable().add(getDataManager().getBoughtStocks()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { stockDbList -> getView().showContent(stockDbList); getView().hideLoading() },
                        { throwable -> Log.e(TAG, throwable.message, throwable) }
                ))
    }

    override fun addStock() {
        getCompositeDisposable().add(getDataManager().downloadStock("TIF")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { downloadedStock -> getDataManager().storeStock(downloadedStock) },
                        { throwable -> Log.e(TAG, throwable.message, throwable) }))
    }

    override fun addMoney() {
        getDataManager().addMoney(10000.00)
        getCompositeDisposable().add(getDataManager().getMoney()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { money -> getView().updateMoney(money.toString()) })
    }

}