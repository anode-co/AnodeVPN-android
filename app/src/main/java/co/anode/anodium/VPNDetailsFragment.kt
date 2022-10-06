@file:Suppress("DEPRECATION")

package co.anode.anodium

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import co.anode.anodium.support.AnodeClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class VPNDetailsFragment : BottomSheetDialogFragment() {
    private var param1: String? = null
    private var param2: String? = null
    private val apiVersion = "0.3"
    private var apiFavoriteUrl = "https://vpn.anode.co/api/$apiVersion/vpn/servers/<server_public_key>/favorite/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val currentView = inflater.inflate(R.layout.fragment_vpn_details, container, false)
        val name = currentView.findViewById<TextView>(R.id.row_name)
        name.text = arguments?.getString("name")
        val countryImageView = currentView.findViewById<ImageView>(R.id.row_country)
        val id = context?.resources?.getIdentifier("ic_"+arguments?.getString("countryCode"), "drawable", requireContext().packageName)
        if (id != null) {
            countryImageView.setImageResource(id)
        }
        val ratingbar = currentView.findViewById<RatingBar>(R.id.list_ratingbar)
        ratingbar.rating = arguments?.getFloat("averageRating")!!
        currentView.findViewById<TextView>(R.id.text_load).text = context?.resources?.getString(R.string.text_load) + arguments?.getString("load") + "%"

        val prefs = context?.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        val favoriteButton = currentView.findViewById<Button>(R.id.button_favorite)
        if (prefs != null) {
            if ((arguments?.getBoolean("isFavorite") == true) || (prefs.getBoolean("favorite_" + arguments?.getString("name"), false))){
                favoriteButton.setBackgroundResource(R.drawable.button_round_fav_small)
            } else {
                favoriteButton.setBackgroundResource(R.drawable.button_round_unfav_small)
            }
        }


        favoriteButton.setOnClickListener {
            if (!prefs?.getBoolean("favorite_" + arguments?.getString("name"), false)!!) {
                AnodeClient.eventLog("Button FAVORITE for " + arguments?.getString("name"))
                toggleFavorite().execute("ADD")
                favoriteButton.setBackgroundResource(R.drawable.button_round_fav_small)
                with(prefs.edit()) {
                    this?.putBoolean("favorite_" + arguments?.getString("name"), true)
                    this?.commit()
                }
            } else {
                AnodeClient.eventLog("Button UNFAVORITE for " + arguments?.getString("name"))
                toggleFavorite().execute("DELETE")
                favoriteButton.setBackgroundResource(R.drawable.button_round_unfav_small)
                with(prefs.edit()) {
                    this?.putBoolean("favorite_" + arguments?.getString("name"), false)
                    this?.commit()
                }
            }
        }
        val connectButton = currentView.findViewById<Button>(R.id.button_smallconnectvpn)
        connectButton.setOnClickListener {
            AnodeClient.eventLog("Button CONNECT to " + arguments?.getString("name"))
            val intent = Intent()
            intent.putExtra("action", "connect")
            intent.putExtra("publickey", arguments?.getString("publicKey"))
            (context as Activity).setResult(Activity.RESULT_OK,intent)
            dismiss()
            (context as Activity).finish()
        }
        val closeButton = currentView.findViewById<ImageButton>(R.id.button_vpndetailsClose)
        closeButton.setOnClickListener {
            dismiss()
        }
        return currentView
    }

    inner class toggleFavorite : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val prefs = context?.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
            val url = apiFavoriteUrl.replace("<server_public_key>", prefs?.getString("ServerPublicKey", "")!!, true)
            return if (params[0]=="ADD") {
                AnodeClient.APIHttpReq(url, "", "POST", needsAuth = true, isRetry = false)
            } else {
                AnodeClient.APIHttpReq(url, "", "DELETE", needsAuth = true, isRetry = false)
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(BuildConfig.APPLICATION_ID, "Received from $apiFavoriteUrl: $result")
            if ((result.isNullOrBlank())) {
                //
            } else {
                try {
                    //
                } catch (e: Exception) {
                    Log.i(BuildConfig.APPLICATION_ID, e.toString())
                }
            }
        }
    }
}