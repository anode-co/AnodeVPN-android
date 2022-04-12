package co.anode.anodium.wallet

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import co.anode.anodium.R
import co.anode.anodium.support.AnodeUtil
import java.io.*

class DebugWalletActivity : AppCompatActivity() {
    private var toBottom = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_wallet)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "PLD Log"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        //Open pld log file and display it
        loadLogFile()

        val refreshButton = findViewById<Button>(R.id.buttonRefresh)
        refreshButton.setOnClickListener {
            loadLogFile()
        }

        val shareButton = findViewById<Button>(R.id.buttonSharePldLog)
        shareButton.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, File(AnodeUtil.filesDirectory + "/" + AnodeUtil.PLD_LOG).readText())
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadLogFile() {
        val `is` = resources.openRawResource(R.raw.ansiup)
        val writer: Writer = StringWriter()
        val buffer = CharArray(1024)
        try {
            val reader: Reader = BufferedReader(InputStreamReader(`is`, "UTF-8"))
            var n: Int
            while (reader.read(buffer).also { n = it } != -1) {
                writer.write(buffer, 0, n)
            }
        } catch (e: java.lang.Exception) {

        } finally {
            `is`.close()
        }
        val ansiupjs = "<script type=\"text/javascript\">$writer</script>"
        val logFile = File(AnodeUtil.filesDirectory +"/"+ AnodeUtil.PLD_LOG)
        val logLines = logFile.readLines()

        var html = "<html><head><style> body {background:#1c2833; color:#dddddd}</style></head><body><div id=\"console\"></div>"
        val js = "<script type=\"text/javascript\">var ansi_up = new AnsiUp; var html = \"\"; "
        var ansiToHtml = ""
        for (i in logLines.indices) {
            ansiToHtml +="html += ansi_up.ansi_to_html(\"${logLines[i]}\")+\"<br>\";"
        }

        val restjs = "var cdiv = document.getElementById(\"console\"); cdiv.innerHTML = html;</script>"
        html += ansiupjs + js + ansiToHtml + restjs
        html += "</body></html>"
        val htmlFile = File("$filesDir/pldlog.html")
        htmlFile.writeText(html, Charsets.UTF_8)
        val webview = findViewById<WebView>(R.id.debugWalletWebview)
        webview.settings.javaScriptEnabled = true
        webview.settings.allowFileAccess = true
        webview.loadUrl("file:///$filesDir/pldlog.html")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}