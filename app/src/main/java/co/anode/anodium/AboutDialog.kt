package co.anode.anodium

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import co.anode.anodium.support.AnodeClient


object AboutDialog {
    private const val LOGTAG = "co.anode.anodium"

    fun show(ctx: Context) {
        Log.i(LOGTAG, "Open About dialog")
        AnodeClient.eventLog("About dialog opened")
        val prefs = ctx.getSharedPreferences("co.anode.anodium", AppCompatActivity.MODE_PRIVATE)
        val checkBoxView: View = View.inflate(ctx, R.layout.about_dialog, null)
        val checkBox = checkBoxView.findViewById<CheckBox>(R.id.about_checkbox)
        checkBox.setOnClickListener {
            with(prefs.edit()) {
                putBoolean("DataConsent", checkBox.isChecked)
                apply()
            }
        }
        checkBox.isChecked = prefs.getBoolean("DataConsent", false)
        val builder: AlertDialog.Builder = AlertDialog.Builder(ctx)
        builder.setTitle("About Anodium VPN")
        builder.setView(checkBoxView)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val alert: AlertDialog = builder.create()
        alert.show()
    }
}