package com.anode.anode

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import java.io.IOException

class FirstFragment : Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? { // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.button_getPeers).setOnClickListener {
            NavHostFragment.findNavController(this@FirstFragment)
                    .navigate(R.id.action_FirstFragment_to_SecondFragment)
            try {
                //val api: AdminApi = AdminApi.from("/data/data/com.anode.anode/files/cjdroute.conf")
                //api.getPeers()
            } catch (e: IOException) {
                Log.e(LOGTAG, "Failed to get peers using Admin API", e)
                e.printStackTrace()
            }
        }
        view.findViewById<View>(R.id.button_ping).setOnClickListener {
            NavHostFragment.findNavController(this@FirstFragment)
                    .navigate(R.id.action_FirstFragment_to_SecondFragment)
            try {
                //val api: AdminApi = AdminApi.from("/data/data/com.anode.anode/files/cjdroute.conf")
                //api.ping()
                //api.getPeers()
            } catch (e: IOException) {
                Log.e(LOGTAG, "Failed to ping using Admin API", e)
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val LOGTAG = "FirstFragment"
    }
}