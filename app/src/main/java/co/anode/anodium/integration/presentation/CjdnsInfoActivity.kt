package com.pkt.app.integration.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.pkt.core.presentation.main.settings.cjdnsinfo.CjdnsInfoFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CjdnsInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    android.R.id.content,
                    CjdnsInfoFragment().apply {
                        arguments = bundleOf("address" to intent.getStringExtra(EXTRA_ADDRESS))
                    }
                )
                .commit()
        }
    }

    companion object {

        private const val EXTRA_ADDRESS = "EXTRA_ADDRESS"

        fun getIntent(context: Context, address: String) = Intent(context, CjdnsInfoActivity::class.java).apply {
            putExtra(EXTRA_ADDRESS, address)
        }
    }
}
