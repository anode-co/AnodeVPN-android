package co.anode.anodium.volley

import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONArray
import org.json.JSONObject

class ServiceVolley : ServiceInterface {
    val TAG = ServiceVolley::class.java.simpleName

    override fun get(url: String, completionHandler: (response: JSONObject?) -> Unit) {
        val jsonObjReq = object : JsonObjectRequest(Method.GET, url, null,
            Response.Listener { response ->
                Log.d(TAG, "/post request OK! Response: $response")
                completionHandler(response)
            },
            Response.ErrorListener { error ->
                VolleyLog.e(TAG, "/post request fail! Error: ${error.message}")
                val jsonError = JSONObject()
                if ((error.networkResponse != null) && (error.networkResponse.data != null )) {
                    val errorString = String(error.networkResponse.data)
                    jsonError.put("error", errorString)
                    completionHandler(jsonError)
                } else if ((!error.message.isNullOrEmpty()) && (error.message!!.contains("java.net.ConnectException: Failed to connect to localhost/127.0.0.1:8080"))) {
                    //pls is not running try to launch it
                    //AnodeUtil.launchPld()
                    jsonError.put("error", "pld not responding")
                    completionHandler(jsonError)
                } else {
                    jsonError.put("error", "unknown")
                    completionHandler(jsonError)
                }
            }) {}
        jsonObjReq.retryPolicy = DefaultRetryPolicy(4000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        BackendVolley.instance?.addToRequestQueue(jsonObjReq, TAG)
    }

    override fun getArray(url: String, completionHandler: (response: JSONArray?) -> Unit) {
        val jsonArrReq = object : JsonArrayRequest(Method.GET, url, null,
            Response.Listener { response ->
                Log.d(TAG, "/post request OK! Response: $response")
                completionHandler(response)
            },
            Response.ErrorListener { error ->
                VolleyLog.e(TAG, "/post request fail! Error: ${error.message}")
                val jsonError = JSONArray()
                if ((error.networkResponse != null) && (error.networkResponse.data != null )) {
                    val errorString = String(error.networkResponse.data)
                    val jsonObj = JSONObject().put("error", errorString)
                    jsonError.put(jsonObj)
                    completionHandler(jsonError)
                } else if ((!error.message.isNullOrEmpty()) && (error.message!!.contains("java.net.ConnectException: Failed to connect to localhost/127.0.0.1:8080"))) {
                    //pls is not running try to launch it
                    //AnodeUtil.launchPld()
                    val jsonObj = JSONObject().put("error", "pld not responding")
                    jsonError.put(jsonObj)
                    completionHandler(jsonError)
                } else {
                    val jsonObj = JSONObject().put("error", "unknown")
                    jsonError.put(jsonObj)
                    completionHandler(jsonError)
                }
            }) {}
        jsonArrReq.retryPolicy = DefaultRetryPolicy(4000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        BackendVolley.instance?.addToRequestQueue(jsonArrReq, TAG)
    }

    override fun post(url: String, params: JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        val jsonObjReq = object : JsonObjectRequest(Method.POST, url, params,
            Response.Listener { response ->
                Log.d(TAG, "/post request OK! Response: $response")
                completionHandler(response)
            },
            Response.ErrorListener { error ->
                VolleyLog.e(TAG, "/post request fail! Error: ${error.message}")
                val jsonError = JSONObject()
                if ((error.networkResponse != null) && (error.networkResponse.data != null)) {
                    val errorString = String(error.networkResponse.data)
                    jsonError.put("error", errorString)
                    completionHandler(jsonError)
                } else if ((!error.message.isNullOrEmpty()) && (error.message!!.contains("java.net.ConnectException: Failed to connect to localhost/127.0.0.1:8080"))) {
                    //pls is not running try to launch it
                    //AnodeUtil.launchPld()
                    jsonError.put("error", "pld not responding")
                    completionHandler(jsonError)
                } else if ((!error.message.isNullOrEmpty()) && (error.message!!.contains("End of input at character 0"))) {
                    //handle empty response indicating recovery worked
                    completionHandler(jsonError)
                } else {
                    jsonError.put("error", "unknown")
                    completionHandler(jsonError)
                }
            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        //Increase timeout for certain REST API calls that take pld more time
        if (url.contains("wallet/create") || url.contains("wallet/changepassphrase"))  {
            jsonObjReq.retryPolicy = DefaultRetryPolicy(60000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        } else if ((url.contains("sendfrom")) || (url.contains("seed/create"))) {
            jsonObjReq.retryPolicy = DefaultRetryPolicy(40000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        } else if (url.contains("wallet/unlock")) {
            jsonObjReq.retryPolicy = DefaultRetryPolicy(15000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        } else {
            jsonObjReq.retryPolicy = DefaultRetryPolicy(4000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        }
        BackendVolley.instance?.addToRequestQueue(jsonObjReq, TAG)
    }
}