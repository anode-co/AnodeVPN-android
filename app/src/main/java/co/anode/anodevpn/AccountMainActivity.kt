package co.anode.anodevpn

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class AccountMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_main)

        val createAccountButton: Button = findViewById(R.id.buttonCreateAccount)
        createAccountButton.setOnClickListener() {
            //TODO:Create account using API and store values to shared prefs
        }
        val skipButton: Button = findViewById(R.id.buttonSkip)
        skipButton.setOnClickListener() {
            //TODO: Go to next activity
        }
    }
}