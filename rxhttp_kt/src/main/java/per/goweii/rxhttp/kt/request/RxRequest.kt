package per.goweii.rxhttp.kt.request

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import per.goweii.rxhttp.kt.core.RxHttp
import per.goweii.rxhttp.kt.core.RxLife
import per.goweii.rxhttp.kt.request.base.BaseResponse
import per.goweii.rxhttp.kt.request.exception.ExceptionHandle
import retrofit2.HttpException

/**
 * <p>文件描述：网络请求<p>
 * <p>@author 烤鱼<p>
 * <p>@date 2020/1/1 0001 <p>
 * <p>@update 2020/1/1 0001<p>
 * <p>版本号：1<p>
 *
 */
class RxRequest<T, E> where E : BaseResponse<T> {

    private var mListener: RequestListener? = null
    private var mRxLife: RxLife? = null

    companion object {
        @JvmStatic
        fun <T, E : BaseResponse<T>> create(observable: Observable<E>): RxRequest<T, E> {
            return RxRequest(observable,null)
        }

        @JvmStatic
        fun <T, E : BaseResponse<T>> createCustom(observable: Observable<T>): RxRequest<T, E> {
            return RxRequest(null,observable)
        }
    }

    private  var mObservable: Observable<E>? = null
    private  var mObservable2: Observable<T>? = null

    private constructor(observable: Observable<E>?,observable2: Observable<T>?) {
        if(observable2 == null){
            this.mObservable = observable!!.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        }else{
            this.mObservable2 = observable2.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        }
    }



    /**
     * 添加请求生命周期的监听
     */
    fun listener(listener: RequestListener): RxRequest<T, E> {
        mListener = listener
        return this
    }

    /**
     * 用于中断请求，管理请求生命周期
     *
     * @param rxLife 详见[RxLife]
     */
    fun autoLife(rxLife: RxLife): RxRequest<T, E> {
        mRxLife = rxLife
        return this
    }

    fun request(callback: ResultCallback<T>): Disposable {
        val disposable = mObservable!!.subscribe({ bean ->
            when {
                bean == null -> {
                    callback.onFailed(-1, "返回为空")
                }
                isSuccess(bean.getCode()).not() -> {
                    callback.onFailed(bean.getCode(), bean.getMsg()?:"请求失败")
                }
                else -> {
                    callback.onSuccess(bean.getCode(), bean.getData())
                }
            }
        }, { t ->

                mListener?.let {
                    var handle:ExceptionHandle? = RxHttp.getRequestSetting()?.getExceptionHandle()
                    if(handle == null){
                        handle = ExceptionHandle(t)
                    }
                    mListener?.onError(handle)
                }
            if(t is HttpException){
                if(RxHttp.getRequestSetting()?.getMultiHttpCode()?.invoke(t.code()) == false){
                    callback.onFailed(t.code(), "${t.message}")
                }
            }else{
                callback.onFailed(-2, "其它异常:${t.message}")
            }
            mListener?.onFinish()
        }, {
            mListener?.onFinish()
        },
            {
                mListener?.onStart() })
        mRxLife?.add(disposable)
        return disposable
    }

    fun customRequest(callback: ( (BaseResponse<T>) -> Unit)): Disposable {
        val disposable = mObservable!!.subscribe({ bean ->
            callback.invoke(bean)
        }, { t ->
            mListener?.let {
                var handle:ExceptionHandle? = RxHttp.getRequestSetting()?.getExceptionHandle()
                if(handle == null){
                    handle = ExceptionHandle(t)
                }
                mListener?.onError(handle)
            }
            mListener?.onFinish()
        }, {
            mListener?.onFinish()
        },
            {
                mListener?.onStart() })
        mRxLife?.add(disposable)
        return disposable
    }

    fun customEntityRequest(callback: ( (T) -> Unit)): Disposable {
        val disposable = mObservable2!!.subscribe({ bean ->
            callback.invoke(bean)
        }, { t ->
            mListener?.let {
                var handle:ExceptionHandle? = RxHttp.getRequestSetting()?.getExceptionHandle()
                if(handle == null){
                    handle = ExceptionHandle(t)
                }
                mListener?.onError(handle)
            }
            mListener?.onFinish()
        }, {
            mListener?.onFinish()
        },
            {
                mListener?.onStart() })
        mRxLife?.add(disposable)
        return disposable
    }

    private fun isSuccess(code: Int): Boolean {
        if (code == RxHttp.getRequestSetting()?.getSuccessCode()) {
            return true
        }
        val codes = RxHttp.getRequestSetting()?.getMultiSuccessCode()
        if (codes == null || codes.isEmpty()) {
            return false
        }
        for (i in codes) {
            if (code == i) {
                return true
            }
        }
        return false
    }


}



interface ResultCallback<E> {
    fun onSuccess(code: Int, data: E?)
    fun onFailed(code: Int, msg: String?)
}



interface RequestListener {
    fun onStart()
    fun onError(handle: ExceptionHandle?)
    fun onFinish()
}