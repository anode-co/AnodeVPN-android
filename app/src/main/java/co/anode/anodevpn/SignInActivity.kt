package co.anode.anodevpn

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
import android.widget.TextView
import androidx.core.text.HtmlCompat

class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        val signup: TextView = findViewById(R.id.textSignUp)
        val link: Spanned = HtmlCompat.fromHtml("don't have an account yer? <a href='#'>Sign Up</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        signup.movementMethod = LinkMovementMethod.getInstance()
        signup.text = link

        val signUpLink = findViewById<TextView>(R.id.textSignUp)
        signUpLink.setMovementMethod(object : TextViewLinkHandler() {
            override fun onLinkClick(url: String?) {
                val accountActivity = Intent(applicationContext, AccountMainActivity::class.java)
                startActivityForResult(accountActivity, 0)
            }
        })
    }

    abstract class TextViewLinkHandler : LinkMovementMethod() {
        override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
            if (event.action != MotionEvent.ACTION_UP) return super.onTouchEvent(widget, buffer, event)
            var x = event.x.toInt()
            var y = event.y.toInt()
            x -= widget.totalPaddingLeft
            y -= widget.totalPaddingTop
            x += widget.scrollX
            y += widget.scrollY
            val layout = widget.layout
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())
            val link = buffer.getSpans(off, off, URLSpan::class.java)
            if (link.size != 0) {
                onLinkClick(link[0].url)
            }
            return true
        }

        abstract fun onLinkClick(url: String?)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0) {
            this.finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}