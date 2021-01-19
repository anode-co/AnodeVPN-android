package co.anode.anodium

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
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class VPNDetailsFragment : BottomSheetDialogFragment() {
    private var param1: String? = null
    private var param2: String? = null
    private val API_VERSION = "0.3"
    private var API_FAVORITE_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/servers/<server_public_key>/favorite/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

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
        val loadtext = currentView.findViewById<TextView>(R.id.text_load)
        loadtext.text = context?.resources?.getString(R.string.text_load) + arguments?.getString("load") + "%"

        val prefs = context?.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
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
                AnodeClient.eventLog(requireContext(), "Button FAVORITE for " + arguments?.getString("name"))
                toggleFavorite().execute("ADD")
                favoriteButton.setBackgroundResource(R.drawable.button_round_fav_small)
                with(prefs?.edit()) {
                    this?.putBoolean("favorite_" + arguments?.getString("name"), true)
                    this?.commit()
                }
            } else {
                AnodeClient.eventLog(requireContext(), "Button UNFAVORITE for " + arguments?.getString("name"))
                toggleFavorite().execute("DELETE")
                favoriteButton.setBackgroundResource(R.drawable.button_round_unfav_small)
                with(prefs?.edit()) {
                    this?.putBoolean("favorite_" + arguments?.getString("name"), false)
                    this?.commit()
                }
            }
        }
        val connectButton = currentView.findViewById<Button>(R.id.button_smallconnectvpn)
        connectButton.setOnClickListener {
            AnodeClient.eventLog(requireContext(), "Button CONNECT to " + arguments?.getString("name"))
            val intent: Intent = Intent()
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

    inner class toggleFavorite() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val prefs = context?.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
            val url = API_FAVORITE_URL.replace("<server_public_key>", prefs?.getString("ServerPublicKey", "")!!, true)
            return if (params[0]=="ADD") {
                AnodeClient.APIHttpReq(url, "", "POST", true, false)
            } else {
                AnodeClient.APIHttpReq(url, "", "DELETE", true, false)
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG, "Received from $API_FAVORITE_URL: $result")
            if ((result.isNullOrBlank())) {
                //
            } else {
                try {
                    //
                } catch (e: Exception) {
                    Log.i(LOGTAG, e.toString())
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                VPNDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}