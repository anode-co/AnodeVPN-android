package co.anode.anodevpn

import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class VerificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

        val msgText = findViewById<TextView>(R.id.textView)
        msgText.text = "We 've sent a verification link to your email.\nPlease check your email."
        setResult(0)
    }
}