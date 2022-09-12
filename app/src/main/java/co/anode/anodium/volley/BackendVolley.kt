package co.anode.anodium.volley

import android.app.Application
import android.text.TextUtils
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BackendVolley : Application() {
    private lateinit var mRequestQueue: RequestQueue
    override fun onCreate() {
        super.onCreate()
        instance = this
        mRequestQueue = Volley.newRequestQueue(applicationContext)
    }

    fun <T> addToRequestQueue(request: Request<T>, tag: String) {
        request.tag = if (TextUtils.isEmpty(tag)) TAG else tag
        mRequestQueue.add(request)
    }

    fun <T> addToRequestQueue(request: Request<T>) {
        request.tag = TAG
        mRequestQueue.add(request)
    }

    fun cancelPendingRequests(tag: Any) {
        mRequestQueue.cancelAll(tag)
    }

    companion object {
        private val TAG = BackendVolley::class.java.simpleName
        @get:Synchronized var instance: BackendVolley? = null
            private set
    }
}