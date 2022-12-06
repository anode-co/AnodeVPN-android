package co.anode.anodium.support

import android.util.Log
import co.anode.anodium.BuildConfig
import timber.log.Timber
import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*


class FileLoggingTree() : Timber.DebugTree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val logTimeStamp: String = SimpleDateFormat("dd.MM.yyyy hh:mm:ss:SSS", Locale.getDefault()).format(Date())
            val logFile = File("${AnodeUtil.filesDirectory}/anodeTimber.txt")
            if (!logFile.exists()) {
                logFile.createNewFile()
            }

            if (logFile.readLines().size > 1000) {
                val smallerLog = logFile.readLines().drop(50)
                Files.write(logFile.toPath(), smallerLog)
            }
            if (t != null) {
                logFile.appendText("$logTimeStamp| $tag| $message T|${t.message}\n")
            } else {
                logFile.appendText("$logTimeStamp| $tag| $message\n")
            }

        } catch (e: Exception) {

            Log.e(BuildConfig.APPLICATION_ID, "Error while logging into file : $e")
        }
    }
}