package com.nitkarsh.learningrx

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.nitkarsh.learningrx.restservices.Model
import com.nitkarsh.learningrx.restservices.RestClient
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import retrofit2.Call
import java.util.concurrent.Callable

class MainActivity : AppCompatActivity() {

    lateinit var call1: Call<List<Model>>
    lateinit var call2: Call<List<Model>>
    var list = arrayListOf<Model>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getSomeWorkDone()
        call1 = RestClient.getApiService().getRepos(userName = "nitkgupta")
        call2 = RestClient.getApiService().getRepos(userName = "nitkgupta")
        getSomeWorkDoneCallable()
        getSomeWorkDoneAgain()
        getSomeWorkDoneFlatMap()
        publishSubject()
        replaySubject()
    }

    fun getObservable(): Observable<String> {
        return Observable.just("Nitkarsh", "Gupta")
    }

    /**
     * getting simple just for emitting single value at a time
     */
    private fun getObserver(): Observer<String> {
        return object : Observer<String> {

            override fun onSubscribe(d: Disposable) {
                Log.e("ONSUBSCRIBE_FIRST", d.isDisposed.toString())
            }

            override fun onNext(value: String) {
                Log.e("VALUE_OBSERVER_FIRST", value)
            }

            override fun onError(e: Throwable) {
                Log.e("ONERROR_FIRST",e.localizedMessage)
            }

            override fun onComplete() {
                println("onComplete FIRST")
            }
        }
    }

    private fun getSomeWorkDone() {
        getObservable().subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(getObserver())
    }

    /**
     * getting observable with callbale type
     */
    private fun getObservableCallable(): Observable<List<Model>>? {
        return Observable.fromCallable(object : Callable<List<Model>> {
            override fun call(): List<Model> {
                if (call1.isExecuted || call1.isCanceled) {
                    return call1.clone().execute().body()!!
                } else {
                    return call1.execute().body()!!
                }
            }
        })
    }

    private fun getObserverCallable(from: String): Observer<List<Model>> {
        return object : Observer<List<Model>> {
            override fun onComplete() {
                Log.e("OnComplete Callable", "You are finished callable")
            }

            override fun onSubscribe(d: Disposable) {
            }

            override fun onNext(t: List<Model>) {
                Log.e("Callable Complete","${t} from ---> $from")
            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    /**
     * @see Observable.map
     * this will call the observable with mapping
     */
    private fun getSomeWorkDoneCallable() {
        getObservableCallable()?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())?.map {
            list.addAll(it)
            Log.e("List_size", list.size.toString())
            list
        }?.subscribe(getObserverCallable("Observable.Map"))
    }

    /**
     * Observable fir fetching user info
     */
    private fun getObservable3WithApi(): Observable<List<Model>> {
        return Observable.fromCallable(object: Callable<List<Model>> {
            override fun call(): List<Model> {
                return if (call2.isExecuted || call2.isCanceled) call2.clone().execute().body()!! else call2.execute().body()!!
            }
        })
    }

    /**
     * @see Observable.zip
     * Zip operator. It combines the result from two and combines to perform some operation and return the result
     */
    private fun getSomeWorkDoneAgain() {
        Observable.zip(getObservableCallable(),getObservable3WithApi(),object: BiFunction<List<Model>,List<Model>,List<Model>> {
            override fun apply(t1: List<Model>, t2: List<Model>): List<Model> {
                return arrayListOf<Model>().apply {
                    addAll(t1)
                    addAll(t2)
                }
            }
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(getObserverCallable("Observable.zip"))
    }

    /**
     * @see flatMap
     * This takes the observable, convert it into single observables.
     * For any modification you can do in that and emit only those values that are eligible for you.
     */
    private fun getSomeWorkDoneFlatMap() {
        getObservableCallable()?.flatMap(object: Function<List<Model>, ObservableSource<List<Model>>> {
            override fun apply(t: List<Model>): ObservableSource<List<Model>> {
                return Observable.fromArray(t)
            }
        })?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())?.map {
            list.addAll(it)
            Log.e("List_size", list.size.toString())
            list
        }?.subscribe(getObserverCallable("Observable.FlatMap"))
    }

    private fun getObserver2TypeString(type: String): Observer<String> {
        return object : Observer<String> {

            override fun onSubscribe(d: Disposable) {
                Log.e("ONSUBSCRIBE_SECOND $type", d.isDisposed.toString())
            }

            override fun onNext(value: String) {
                Log.e("VALUE_OBSERVER_SECOND $type", value)
            }

            override fun onError(e: Throwable) {
                Log.e("ONERROR_SECOND $type",e.localizedMessage)
            }

            override fun onComplete() {
                println("onComplete_SECOND $type")
            }
        }
    }

    //--------------------SUBJECT------------------------//

    /**
     * @see PublishSubject
     * It can act add observer at any given time.
     * Like some observer can start observing after 5 sec from current data.
     * It emits subsequent data of the Observable Source at the time of subscription
     */
    private fun publishSubject() {
        val publishSubject : PublishSubject<String> = PublishSubject.create()
        publishSubject.subscribe(getObserver())
        publishSubject.onNext("Nitkarsh")
        publishSubject.onNext("Gupta")
        publishSubject.onNext("Finally")
        publishSubject.subscribe(getObserver2TypeString("publishSubject")) // this will receive item emitted after this point only
        publishSubject.onNext("Hello brooo")
        publishSubject.onComplete()
    }

    /**
     * @see ReplaySubject
     * It can also add observer at any time
     * It differs from PublishSubject in the way that observer will get all items emitted
     * even the items that were emitted before observer is added
     */
    private fun replaySubject() {
        var replaySubject: ReplaySubject<String> = ReplaySubject.create()
        replaySubject.subscribe(getObserver())
        replaySubject.onNext("Nitkarsh")
        replaySubject.onNext("Gupta")
        replaySubject.onNext("Finally")
        replaySubject.subscribe(getObserver2TypeString("replaySubject")) // this will receive every item emitted that may be emitted before this point
        replaySubject.onNext("Hello brooo")
        replaySubject.onComplete()
    }


}
