package co.anode.anodium.wallet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.R
import java.io.File

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

    private fun loadLogFile() {
        val ansiupjs = "  <script type=\"text/javascript\">\n" +
                "(function (root, factory) {\n" +
                "    if (typeof define === 'function' && define.amd) {\n" +
                "        // AMD. Register as an anonymous module.\n" +
                "        define(['exports'], factory);\n" +
                "    } else if (typeof exports === 'object' && typeof exports.nodeName !== 'string') {\n" +
                "        // CommonJS\n" +
                "        factory(exports);\n" +
                "    } else {\n" +
                "        // Browser globals\n" +
                "        var exp = {};\n" +
                "        factory(exp);\n" +
                "        root.AnsiUp = exp.default;\n" +
                "    }\n" +
                "}(this, function (exports) {\n" +
                "\"use strict\";\n" +
                "var __makeTemplateObject = (this && this.__makeTemplateObject) || function (cooked, raw) {\n" +
                "    if (Object.defineProperty) { Object.defineProperty(cooked, \"raw\", { value: raw }); } else { cooked.raw = raw; }\n" +
                "    return cooked;\n" +
                "};\n" +
                "var PacketKind;\n" +
                "(function (PacketKind) {\n" +
                "    PacketKind[PacketKind[\"EOS\"] = 0] = \"EOS\";\n" +
                "    PacketKind[PacketKind[\"Text\"] = 1] = \"Text\";\n" +
                "    PacketKind[PacketKind[\"Incomplete\"] = 2] = \"Incomplete\";\n" +
                "    PacketKind[PacketKind[\"ESC\"] = 3] = \"ESC\";\n" +
                "    PacketKind[PacketKind[\"Unknown\"] = 4] = \"Unknown\";\n" +
                "    PacketKind[PacketKind[\"SGR\"] = 5] = \"SGR\";\n" +
                "    PacketKind[PacketKind[\"OSCURL\"] = 6] = \"OSCURL\";\n" +
                "})(PacketKind || (PacketKind = {}));\n" +
                "var AnsiUp = (function () {\n" +
                "    function AnsiUp() {\n" +
                "        this.VERSION = \"5.1.0\";\n" +
                "        this.setup_palettes();\n" +
                "        this._use_classes = false;\n" +
                "        this.bold = false;\n" +
                "        this.italic = false;\n" +
                "        this.underline = false;\n" +
                "        this.fg = this.bg = null;\n" +
                "        this._buffer = '';\n" +
                "        this._url_whitelist = { 'http': 1, 'https': 1 };\n" +
                "    }\n" +
                "    Object.defineProperty(AnsiUp.prototype, \"use_classes\", {\n" +
                "        get: function () {\n" +
                "            return this._use_classes;\n" +
                "        },\n" +
                "        set: function (arg) {\n" +
                "            this._use_classes = arg;\n" +
                "        },\n" +
                "        enumerable: false,\n" +
                "        configurable: true\n" +
                "    });\n" +
                "    Object.defineProperty(AnsiUp.prototype, \"url_whitelist\", {\n" +
                "        get: function () {\n" +
                "            return this._url_whitelist;\n" +
                "        },\n" +
                "        set: function (arg) {\n" +
                "            this._url_whitelist = arg;\n" +
                "        },\n" +
                "        enumerable: false,\n" +
                "        configurable: true\n" +
                "    });\n" +
                "    AnsiUp.prototype.setup_palettes = function () {\n" +
                "        var _this = this;\n" +
                "        this.ansi_colors =\n" +
                "            [\n" +
                "                [\n" +
                "                    { rgb: [0, 0, 0], class_name: \"ansi-black\" },\n" +
                "                    { rgb: [187, 0, 0], class_name: \"ansi-red\" },\n" +
                "                    { rgb: [0, 187, 0], class_name: \"ansi-green\" },\n" +
                "                    { rgb: [187, 187, 0], class_name: \"ansi-yellow\" },\n" +
                "                    { rgb: [0, 0, 187], class_name: \"ansi-blue\" },\n" +
                "                    { rgb: [187, 0, 187], class_name: \"ansi-magenta\" },\n" +
                "                    { rgb: [0, 187, 187], class_name: \"ansi-cyan\" },\n" +
                "                    { rgb: [255, 255, 255], class_name: \"ansi-white\" }\n" +
                "                ],\n" +
                "                [\n" +
                "                    { rgb: [85, 85, 85], class_name: \"ansi-bright-black\" },\n" +
                "                    { rgb: [255, 85, 85], class_name: \"ansi-bright-red\" },\n" +
                "                    { rgb: [0, 255, 0], class_name: \"ansi-bright-green\" },\n" +
                "                    { rgb: [255, 255, 85], class_name: \"ansi-bright-yellow\" },\n" +
                "                    { rgb: [85, 85, 255], class_name: \"ansi-bright-blue\" },\n" +
                "                    { rgb: [255, 85, 255], class_name: \"ansi-bright-magenta\" },\n" +
                "                    { rgb: [85, 255, 255], class_name: \"ansi-bright-cyan\" },\n" +
                "                    { rgb: [255, 255, 255], class_name: \"ansi-bright-white\" }\n" +
                "                ]\n" +
                "            ];\n" +
                "        this.palette_256 = [];\n" +
                "        this.ansi_colors.forEach(function (palette) {\n" +
                "            palette.forEach(function (rec) {\n" +
                "                _this.palette_256.push(rec);\n" +
                "            });\n" +
                "        });\n" +
                "        var levels = [0, 95, 135, 175, 215, 255];\n" +
                "        for (var r = 0; r < 6; ++r) {\n" +
                "            for (var g = 0; g < 6; ++g) {\n" +
                "                for (var b = 0; b < 6; ++b) {\n" +
                "                    var col = { rgb: [levels[r], levels[g], levels[b]], class_name: 'truecolor' };\n" +
                "                    this.palette_256.push(col);\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        var grey_level = 8;\n" +
                "        for (var i = 0; i < 24; ++i, grey_level += 10) {\n" +
                "            var gry = { rgb: [grey_level, grey_level, grey_level], class_name: 'truecolor' };\n" +
                "            this.palette_256.push(gry);\n" +
                "        }\n" +
                "    };\n" +
                "    AnsiUp.prototype.escape_txt_for_html = function (txt) {\n" +
                "        return txt.replace(/[&<>\"']/gm, function (str) {\n" +
                "            if (str === \"&\")\n" +
                "                return \"&amp;\";\n" +
                "            if (str === \"<\")\n" +
                "                return \"&lt;\";\n" +
                "            if (str === \">\")\n" +
                "                return \"&gt;\";\n" +
                "            if (str === \"\\\"\")\n" +
                "                return \"&quot;\";\n" +
                "            if (str === \"'\")\n" +
                "                return \"&#x27;\";\n" +
                "        });\n" +
                "    };\n" +
                "    AnsiUp.prototype.append_buffer = function (txt) {\n" +
                "        var str = this._buffer + txt;\n" +
                "        this._buffer = str;\n" +
                "    };\n" +
                "    AnsiUp.prototype.get_next_packet = function () {\n" +
                "        var pkt = {\n" +
                "            kind: PacketKind.EOS,\n" +
                "            text: '',\n" +
                "            url: ''\n" +
                "        };\n" +
                "        var len = this._buffer.length;\n" +
                "        if (len == 0)\n" +
                "            return pkt;\n" +
                "        var pos = this._buffer.indexOf(\"\\x1B\");\n" +
                "        if (pos == -1) {\n" +
                "            pkt.kind = PacketKind.Text;\n" +
                "            pkt.text = this._buffer;\n" +
                "            this._buffer = '';\n" +
                "            return pkt;\n" +
                "        }\n" +
                "        if (pos > 0) {\n" +
                "            pkt.kind = PacketKind.Text;\n" +
                "            pkt.text = this._buffer.slice(0, pos);\n" +
                "            this._buffer = this._buffer.slice(pos);\n" +
                "            return pkt;\n" +
                "        }\n" +
                "        if (pos == 0) {\n" +
                "            if (len == 1) {\n" +
                "                pkt.kind = PacketKind.Incomplete;\n" +
                "                return pkt;\n" +
                "            }\n" +
                "            var next_char = this._buffer.charAt(1);\n" +
                "            if ((next_char != '[') && (next_char != ']')) {\n" +
                "                pkt.kind = PacketKind.ESC;\n" +
                "                pkt.text = this._buffer.slice(0, 1);\n" +
                "                this._buffer = this._buffer.slice(1);\n" +
                "                return pkt;\n" +
                "            }\n" +
                "            if (next_char == '[') {\n" +
                "                if (!this._csi_regex) {\n" +
                "                    this._csi_regex = rgx(__makeTemplateObject([\"\\n                        ^                           # beginning of line\\n                                                    #\\n                                                    # First attempt\\n                        (?:                         # legal sequence\\n                          \\u001B[                      # CSI\\n                          ([<-?]?)              # private-mode char\\n                          ([d;]*)                    # any digits or semicolons\\n                          ([ -/]?               # an intermediate modifier\\n                          [@-~])                # the command\\n                        )\\n                        |                           # alternate (second attempt)\\n                        (?:                         # illegal sequence\\n                          \\u001B[                      # CSI\\n                          [ -~]*                # anything legal\\n                          ([\\0-\\u001F:])              # anything illegal\\n                        )\\n                    \"], [\"\\n                        ^                           # beginning of line\\n                                                    #\\n                                                    # First attempt\\n                        (?:                         # legal sequence\\n                          \\\\x1b\\\\[                      # CSI\\n                          ([\\\\x3c-\\\\x3f]?)              # private-mode char\\n                          ([\\\\d;]*)                    # any digits or semicolons\\n                          ([\\\\x20-\\\\x2f]?               # an intermediate modifier\\n                          [\\\\x40-\\\\x7e])                # the command\\n                        )\\n                        |                           # alternate (second attempt)\\n                        (?:                         # illegal sequence\\n                          \\\\x1b\\\\[                      # CSI\\n                          [\\\\x20-\\\\x7e]*                # anything legal\\n                          ([\\\\x00-\\\\x1f:])              # anything illegal\\n                        )\\n                    \"]));\n" +
                "                }\n" +
                "                var match = this._buffer.match(this._csi_regex);\n" +
                "                if (match === null) {\n" +
                "                    pkt.kind = PacketKind.Incomplete;\n" +
                "                    return pkt;\n" +
                "                }\n" +
                "                if (match[4]) {\n" +
                "                    pkt.kind = PacketKind.ESC;\n" +
                "                    pkt.text = this._buffer.slice(0, 1);\n" +
                "                    this._buffer = this._buffer.slice(1);\n" +
                "                    return pkt;\n" +
                "                }\n" +
                "                if ((match[1] != '') || (match[3] != 'm'))\n" +
                "                    pkt.kind = PacketKind.Unknown;\n" +
                "                else\n" +
                "                    pkt.kind = PacketKind.SGR;\n" +
                "                pkt.text = match[2];\n" +
                "                var rpos = match[0].length;\n" +
                "                this._buffer = this._buffer.slice(rpos);\n" +
                "                return pkt;\n" +
                "            }\n" +
                "            if (next_char == ']') {\n" +
                "                if (len < 4) {\n" +
                "                    pkt.kind = PacketKind.Incomplete;\n" +
                "                    return pkt;\n" +
                "                }\n" +
                "                if ((this._buffer.charAt(2) != '8')\n" +
                "                    || (this._buffer.charAt(3) != ';')) {\n" +
                "                    pkt.kind = PacketKind.ESC;\n" +
                "                    pkt.text = this._buffer.slice(0, 1);\n" +
                "                    this._buffer = this._buffer.slice(1);\n" +
                "                    return pkt;\n" +
                "                }\n" +
                "                if (!this._osc_st) {\n" +
                "                    this._osc_st = rgxG(__makeTemplateObject([\"\\n                        (?:                         # legal sequence\\n                          (\\u001B\\\\)                    # ESC                           |                           # alternate\\n                          (\\u0007)                      # BEL (what xterm did)\\n                        )\\n                        |                           # alternate (second attempt)\\n                        (                           # illegal sequence\\n                          [\\0-\\u0006]                 # anything illegal\\n                          |                           # alternate\\n                          [\\b-\\u001A]                 # anything illegal\\n                          |                           # alternate\\n                          [\\u001C-\\u001F]                 # anything illegal\\n                        )\\n                    \"], [\"\\n                        (?:                         # legal sequence\\n                          (\\\\x1b\\\\\\\\)                    # ESC \\\\\\n                          |                           # alternate\\n                          (\\\\x07)                      # BEL (what xterm did)\\n                        )\\n                        |                           # alternate (second attempt)\\n                        (                           # illegal sequence\\n                          [\\\\x00-\\\\x06]                 # anything illegal\\n                          |                           # alternate\\n                          [\\\\x08-\\\\x1a]                 # anything illegal\\n                          |                           # alternate\\n                          [\\\\x1c-\\\\x1f]                 # anything illegal\\n                        )\\n                    \"]));\n" +
                "                }\n" +
                "                this._osc_st.lastIndex = 0;\n" +
                "                {\n" +
                "                    var match_1 = this._osc_st.exec(this._buffer);\n" +
                "                    if (match_1 === null) {\n" +
                "                        pkt.kind = PacketKind.Incomplete;\n" +
                "                        return pkt;\n" +
                "                    }\n" +
                "                    if (match_1[3]) {\n" +
                "                        pkt.kind = PacketKind.ESC;\n" +
                "                        pkt.text = this._buffer.slice(0, 1);\n" +
                "                        this._buffer = this._buffer.slice(1);\n" +
                "                        return pkt;\n" +
                "                    }\n" +
                "                }\n" +
                "                {\n" +
                "                    var match_2 = this._osc_st.exec(this._buffer);\n" +
                "                    if (match_2 === null) {\n" +
                "                        pkt.kind = PacketKind.Incomplete;\n" +
                "                        return pkt;\n" +
                "                    }\n" +
                "                    if (match_2[3]) {\n" +
                "                        pkt.kind = PacketKind.ESC;\n" +
                "                        pkt.text = this._buffer.slice(0, 1);\n" +
                "                        this._buffer = this._buffer.slice(1);\n" +
                "                        return pkt;\n" +
                "                    }\n" +
                "                }\n" +
                "                if (!this._osc_regex) {\n" +
                "                    this._osc_regex = rgx(__makeTemplateObject([\"\\n                        ^                           # beginning of line\\n                                                    #\\n                        \\u001B]8;                    # OSC Hyperlink\\n                        [ -:<-~]*       # params (excluding ;)\\n                        ;                           # end of params\\n                        ([!-~]{0,512})        # URL capture\\n                        (?:                         # ST\\n                          (?:\\u001B\\\\)                  # ESC                           |                           # alternate\\n                          (?:\\u0007)                    # BEL (what xterm did)\\n                        )\\n                        ([ -~]+)              # TEXT capture\\n                        \\u001B]8;;                   # OSC Hyperlink End\\n                        (?:                         # ST\\n                          (?:\\u001B\\\\)                  # ESC                           |                           # alternate\\n                          (?:\\u0007)                    # BEL (what xterm did)\\n                        )\\n                    \"], [\"\\n                        ^                           # beginning of line\\n                                                    #\\n                        \\\\x1b\\\\]8;                    # OSC Hyperlink\\n                        [\\\\x20-\\\\x3a\\\\x3c-\\\\x7e]*       # params (excluding ;)\\n                        ;                           # end of params\\n                        ([\\\\x21-\\\\x7e]{0,512})        # URL capture\\n                        (?:                         # ST\\n                          (?:\\\\x1b\\\\\\\\)                  # ESC \\\\\\n                          |                           # alternate\\n                          (?:\\\\x07)                    # BEL (what xterm did)\\n                        )\\n                        ([\\\\x20-\\\\x7e]+)              # TEXT capture\\n                        \\\\x1b\\\\]8;;                   # OSC Hyperlink End\\n                        (?:                         # ST\\n                          (?:\\\\x1b\\\\\\\\)                  # ESC \\\\\\n                          |                           # alternate\\n                          (?:\\\\x07)                    # BEL (what xterm did)\\n                        )\\n                    \"]));\n" +
                "                }\n" +
                "                var match = this._buffer.match(this._osc_regex);\n" +
                "                if (match === null) {\n" +
                "                    pkt.kind = PacketKind.ESC;\n" +
                "                    pkt.text = this._buffer.slice(0, 1);\n" +
                "                    this._buffer = this._buffer.slice(1);\n" +
                "                    return pkt;\n" +
                "                }\n" +
                "                pkt.kind = PacketKind.OSCURL;\n" +
                "                pkt.url = match[1];\n" +
                "                pkt.text = match[2];\n" +
                "                var rpos = match[0].length;\n" +
                "                this._buffer = this._buffer.slice(rpos);\n" +
                "                return pkt;\n" +
                "            }\n" +
                "        }\n" +
                "    };\n" +
                "    AnsiUp.prototype.ansi_to_html = function (txt) {\n" +
                "        this.append_buffer(txt);\n" +
                "        var blocks = [];\n" +
                "        while (true) {\n" +
                "            var packet = this.get_next_packet();\n" +
                "            if ((packet.kind == PacketKind.EOS)\n" +
                "                || (packet.kind == PacketKind.Incomplete))\n" +
                "                break;\n" +
                "            if ((packet.kind == PacketKind.ESC)\n" +
                "                || (packet.kind == PacketKind.Unknown))\n" +
                "                continue;\n" +
                "            if (packet.kind == PacketKind.Text)\n" +
                "                blocks.push(this.transform_to_html(this.with_state(packet)));\n" +
                "            else if (packet.kind == PacketKind.SGR)\n" +
                "                this.process_ansi(packet);\n" +
                "            else if (packet.kind == PacketKind.OSCURL)\n" +
                "                blocks.push(this.process_hyperlink(packet));\n" +
                "        }\n" +
                "        return blocks.join(\"\");\n" +
                "    };\n" +
                "    AnsiUp.prototype.with_state = function (pkt) {\n" +
                "        return { bold: this.bold, italic: this.italic, underline: this.underline, fg: this.fg, bg: this.bg, text: pkt.text };\n" +
                "    };\n" +
                "    AnsiUp.prototype.process_ansi = function (pkt) {\n" +
                "        var sgr_cmds = pkt.text.split(';');\n" +
                "        while (sgr_cmds.length > 0) {\n" +
                "            var sgr_cmd_str = sgr_cmds.shift();\n" +
                "            var num = parseInt(sgr_cmd_str, 10);\n" +
                "            if (isNaN(num) || num === 0) {\n" +
                "                this.fg = this.bg = null;\n" +
                "                this.bold = false;\n" +
                "                this.italic = false;\n" +
                "                this.underline = false;\n" +
                "            }\n" +
                "            else if (num === 1) {\n" +
                "                this.bold = true;\n" +
                "            }\n" +
                "            else if (num === 3) {\n" +
                "                this.italic = true;\n" +
                "            }\n" +
                "            else if (num === 4) {\n" +
                "                this.underline = true;\n" +
                "            }\n" +
                "            else if (num === 22) {\n" +
                "                this.bold = false;\n" +
                "            }\n" +
                "            else if (num === 23) {\n" +
                "                this.italic = false;\n" +
                "            }\n" +
                "            else if (num === 24) {\n" +
                "                this.underline = false;\n" +
                "            }\n" +
                "            else if (num === 39) {\n" +
                "                this.fg = null;\n" +
                "            }\n" +
                "            else if (num === 49) {\n" +
                "                this.bg = null;\n" +
                "            }\n" +
                "            else if ((num >= 30) && (num < 38)) {\n" +
                "                this.fg = this.ansi_colors[0][(num - 30)];\n" +
                "            }\n" +
                "            else if ((num >= 40) && (num < 48)) {\n" +
                "                this.bg = this.ansi_colors[0][(num - 40)];\n" +
                "            }\n" +
                "            else if ((num >= 90) && (num < 98)) {\n" +
                "                this.fg = this.ansi_colors[1][(num - 90)];\n" +
                "            }\n" +
                "            else if ((num >= 100) && (num < 108)) {\n" +
                "                this.bg = this.ansi_colors[1][(num - 100)];\n" +
                "            }\n" +
                "            else if (num === 38 || num === 48) {\n" +
                "                if (sgr_cmds.length > 0) {\n" +
                "                    var is_foreground = (num === 38);\n" +
                "                    var mode_cmd = sgr_cmds.shift();\n" +
                "                    if (mode_cmd === '5' && sgr_cmds.length > 0) {\n" +
                "                        var palette_index = parseInt(sgr_cmds.shift(), 10);\n" +
                "                        if (palette_index >= 0 && palette_index <= 255) {\n" +
                "                            if (is_foreground)\n" +
                "                                this.fg = this.palette_256[palette_index];\n" +
                "                            else\n" +
                "                                this.bg = this.palette_256[palette_index];\n" +
                "                        }\n" +
                "                    }\n" +
                "                    if (mode_cmd === '2' && sgr_cmds.length > 2) {\n" +
                "                        var r = parseInt(sgr_cmds.shift(), 10);\n" +
                "                        var g = parseInt(sgr_cmds.shift(), 10);\n" +
                "                        var b = parseInt(sgr_cmds.shift(), 10);\n" +
                "                        if ((r >= 0 && r <= 255) && (g >= 0 && g <= 255) && (b >= 0 && b <= 255)) {\n" +
                "                            var c = { rgb: [r, g, b], class_name: 'truecolor' };\n" +
                "                            if (is_foreground)\n" +
                "                                this.fg = c;\n" +
                "                            else\n" +
                "                                this.bg = c;\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    };\n" +
                "    AnsiUp.prototype.transform_to_html = function (fragment) {\n" +
                "        var txt = fragment.text;\n" +
                "        if (txt.length === 0)\n" +
                "            return txt;\n" +
                "        txt = this.escape_txt_for_html(txt);\n" +
                "        if (!fragment.bold && !fragment.italic && !fragment.underline && fragment.fg === null && fragment.bg === null)\n" +
                "            return txt;\n" +
                "        var styles = [];\n" +
                "        var classes = [];\n" +
                "        var fg = fragment.fg;\n" +
                "        var bg = fragment.bg;\n" +
                "        if (fragment.bold)\n" +
                "            styles.push('font-weight:bold');\n" +
                "        if (fragment.italic)\n" +
                "            styles.push('font-style:italic');\n" +
                "        if (fragment.underline)\n" +
                "            styles.push('text-decoration:underline');\n" +
                "        if (!this._use_classes) {\n" +
                "            if (fg)\n" +
                "                styles.push(\"color:rgb(\" + fg.rgb.join(',') + \")\");\n" +
                "            if (bg)\n" +
                "                styles.push(\"background-color:rgb(\" + bg.rgb + \")\");\n" +
                "        }\n" +
                "        else {\n" +
                "            if (fg) {\n" +
                "                if (fg.class_name !== 'truecolor') {\n" +
                "                    classes.push(fg.class_name + \"-fg\");\n" +
                "                }\n" +
                "                else {\n" +
                "                    styles.push(\"color:rgb(\" + fg.rgb.join(',') + \")\");\n" +
                "                }\n" +
                "            }\n" +
                "            if (bg) {\n" +
                "                if (bg.class_name !== 'truecolor') {\n" +
                "                    classes.push(bg.class_name + \"-bg\");\n" +
                "                }\n" +
                "                else {\n" +
                "                    styles.push(\"background-color:rgb(\" + bg.rgb.join(',') + \")\");\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        var class_string = '';\n" +
                "        var style_string = '';\n" +
                "        if (classes.length)\n" +
                "            class_string = \" class=\\\"\" + classes.join(' ') + \"\\\"\";\n" +
                "        if (styles.length)\n" +
                "            style_string = \" style=\\\"\" + styles.join(';') + \"\\\"\";\n" +
                "        return \"<span\" + style_string + class_string + \">\" + txt + \"</span>\";\n" +
                "    };\n" +
                "    ;\n" +
                "    AnsiUp.prototype.process_hyperlink = function (pkt) {\n" +
                "        var parts = pkt.url.split(':');\n" +
                "        if (parts.length < 1)\n" +
                "            return '';\n" +
                "        if (!this._url_whitelist[parts[0]])\n" +
                "            return '';\n" +
                "        var result = \"<a href=\\\"\" + this.escape_txt_for_html(pkt.url) + \"\\\">\" + this.escape_txt_for_html(pkt.text) + \"</a>\";\n" +
                "        return result;\n" +
                "    };\n" +
                "    return AnsiUp;\n" +
                "}());\n" +
                "function rgx(tmplObj) {\n" +
                "    var subst = [];\n" +
                "    for (var _i = 1; _i < arguments.length; _i++) {\n" +
                "        subst[_i - 1] = arguments[_i];\n" +
                "    }\n" +
                "    var regexText = tmplObj.raw[0];\n" +
                "    var wsrgx = /^\\s+|\\s+\\n|\\s*#[\\s\\S]*?\\n|\\n/gm;\n" +
                "    var txt2 = regexText.replace(wsrgx, '');\n" +
                "    return new RegExp(txt2);\n" +
                "}\n" +
                "function rgxG(tmplObj) {\n" +
                "    var subst = [];\n" +
                "    for (var _i = 1; _i < arguments.length; _i++) {\n" +
                "        subst[_i - 1] = arguments[_i];\n" +
                "    }\n" +
                "    var regexText = tmplObj.raw[0];\n" +
                "    var wsrgx = /^\\s+|\\s+\\n|\\s*#[\\s\\S]*?\\n|\\n/gm;\n" +
                "    var txt2 = regexText.replace(wsrgx, '');\n" +
                "    return new RegExp(txt2, 'g');\n" +
                "}\n" +
                "//# sourceMappingURL=ansi_up.js.map\n" +
                "    Object.defineProperty(exports, \"__esModule\", { value: true });\n" +
                "    exports.default = AnsiUp;\n" +
                "}));\n" +
                "  </script>"
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