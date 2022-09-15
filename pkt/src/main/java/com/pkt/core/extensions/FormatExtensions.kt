package com.pkt.core.extensions

import android.content.Context
import com.pkt.core.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

private val LOCALE = Locale.US

private val PKT_UNITS = mapOf(
    "PKT" to 1_073_741_824L,
    "mPKT" to 1_073_741L,
    "μPKT" to 1_073L,
    "nPKT" to 1L,
)

private const val USD = "USD"

private const val PKT_DIGITS = 9

private val PKT_FORMATTER by lazy {
    (NumberFormat.getInstance(LOCALE) as DecimalFormat).apply {
        isGroupingUsed = true
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
}

private val USD_FORMATTER by lazy {
    (NumberFormat.getInstance(LOCALE) as DecimalFormat).apply {
        isGroupingUsed = true
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        positiveSuffix = " $USD"
        negativeSuffix = " $USD"
    }
}

private val DATE_TIME_PARSER by lazy {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", LOCALE)
}

private val DATE_TIME_FORMATTER by lazy {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", LOCALE)
}

private val DATE_SHORT_FORMATTER by lazy {
    SimpleDateFormat("MMMM dd", LOCALE)
}

private val TIME_FORMATTER by lazy {
    SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, LOCALE)
}

fun String?.formatPkt(digits: Int = 0): String =
    when (digits) {
        0 -> PKT_FORMATTER

        else -> (PKT_FORMATTER.clone() as DecimalFormat).apply {
            maximumFractionDigits = digits
            minimumFractionDigits = digits
        }
    }.format(this.toBigDecimalSafety())

fun Double?.formatPkt(): String {
    val amount = this.toBigDecimalSafety().movePointRight(PKT_DIGITS).toLong()
    val unit = PKT_UNITS.entries.find { amount >= it.value } ?: PKT_UNITS.entries.last()
    val value = BigDecimal(amount).divide(BigDecimal(unit.value), 2, RoundingMode.HALF_EVEN)
    return (PKT_FORMATTER.clone() as DecimalFormat).apply {
        decimalFormatSymbols = decimalFormatSymbols.apply {
            positiveSuffix = " ${unit.key}"
            negativeSuffix = " ${unit.key}"
        }
    }.format(value)
}

fun Long.formatPkt(): String {
    val unit = PKT_UNITS.entries.find { this >= it.value } ?: PKT_UNITS.entries.last()
    val value = BigDecimal(this).divide(BigDecimal(unit.value), 2, RoundingMode.HALF_EVEN)
    return (PKT_FORMATTER.clone() as DecimalFormat).apply {
        decimalFormatSymbols = decimalFormatSymbols.apply {
            positiveSuffix = " ${unit.key}"
            negativeSuffix = " ${unit.key}"
        }
    }.format(value)
}

fun Long.toPKT(): BigDecimal {
    val unit = PKT_UNITS.entries.find { this >= it.value } ?: PKT_UNITS.entries.last()
    return BigDecimal(this).divide(BigDecimal(unit.value), 2, RoundingMode.HALF_EVEN)
}


fun String?.formatUsd(): String = USD_FORMATTER.format(this.toBigDecimalSafety())

fun LocalDateTime.formatDateTime(): String = DATE_TIME_FORMATTER.format(this.toDate())

fun String.formatDateTime(): String = DATE_TIME_FORMATTER.format(this.toDateSafety())

fun LocalDateTime.formatDateShort(): String = DATE_SHORT_FORMATTER.format(this.toDate())

fun LocalDateTime.formatTime(): String = TIME_FORMATTER.format(this.toDate())

fun Long.formatBytes(context: Context): String {
    val mb = this / 1_000_000L
    return if (mb > 0) {
        "$mb ${context.getString(R.string.mb)}"
    } else {
        "${this / 1000} ${context.getString(R.string.kb)}"
    }
}

fun String.firstLetterUppercase(): String = this.substring(0, 1).uppercase() + this.substring(1)

fun Int?.formatSeconds(): String {
    var seconds = this ?: 0
    val minutes = seconds / 60
    seconds %= 60
    return "%d:%02d".format(minutes, seconds)
}

private fun String?.toBigDecimalSafety(): BigDecimal = this?.toBigDecimalOrNull() ?: BigDecimal.ZERO

private fun Double?.toBigDecimalSafety(): BigDecimal = this?.toBigDecimal() ?: BigDecimal.ZERO

private fun LocalDateTime.toDate(): Date = Date.from(this.atZone(ZoneId.systemDefault()).toInstant())

private fun String.toDateSafety(): Date = DATE_TIME_PARSER.parse(this)!!
