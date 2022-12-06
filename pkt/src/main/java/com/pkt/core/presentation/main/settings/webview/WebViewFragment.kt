package com.pkt.core.presentation.main.settings.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentChangePinBinding
import com.pkt.core.databinding.FragmentWebviewBinding
import com.pkt.core.extensions.clearFocusOnActionDone
import com.pkt.core.extensions.doOnClick
import com.pkt.core.extensions.doOnTextChanged
import com.pkt.core.extensions.setError
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.UiEvent
import com.pkt.core.presentation.common.state.state.CommonState
import com.pkt.core.presentation.main.wallet.WalletState
import dagger.hilt.android.AndroidEntryPoint
import java.io.*

@AndroidEntryPoint
class WebViewFragment : StateFragment<WebViewState>(R.layout.fragment_webview) {

    private val viewBinding by viewBinding(FragmentWebviewBinding::bind)

    override val viewModel: WebViewViewModel by viewModels()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            webview.settings.allowContentAccess = true
            webview.settings.allowFileAccess = true
            webview.settings.javaScriptEnabled = true
        }
    }

    override fun handleState(state: WebViewState) {
        if (state.html.contains("pldlog")) {
            getpldLog()
        }
        viewBinding.webview.loadUrl(state.html)
    }

    fun getpldLog() {
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
        val logFile = File( "${activity?.filesDir}/pldlog.txt")
        val logLines = logFile.readLines()
        var html = "<html><head><style> body {background:#141528; color:#dddddd}</style></head><body><div id=\"console\"></div>"
        val js = "<script type=\"text/javascript\">var ansi_up = new AnsiUp; var html = \"\"; "
        var ansiToHtml = ""
        for (i in logLines.indices) {
            ansiToHtml +="html += ansi_up.ansi_to_html(\"${logLines[i]}\")+\"<br>\";"
        }

        val restjs = "var cdiv = document.getElementById(\"console\"); cdiv.innerHTML = html;</script>"
        html += ansiupjs + js + ansiToHtml + restjs
        html += "</body></html>"
        viewModel.savePldLog(html)
    }
}
