package com.pkt.core.util

import java.util.*

object CountryUtil {

    private const val REGIONAL_INDICATOR_OFFSET = 0x1F1A5

    private fun getCodeByCharacter(character: Char): String =
        String(Character.toChars(REGIONAL_INDICATOR_OFFSET + character.code))

    fun getCountryFlag(countryCode: String): String =
        if (countryCode.length == 2) {
            getCodeByCharacter(countryCode[0]) + getCodeByCharacter(countryCode[1])
        } else {
            ""
        }

    fun getCountryName(countryCode: String): String = Locale("", countryCode).getDisplayCountry(Locale.US)
}
