package com.pkt.domain.repository

interface GeneralRepository {
    fun submitErrorLogs(): Boolean
    fun enablePreReleaseUpgrade(value: Boolean)
    fun getPreReleaseUpgrade(): Boolean
    fun removePIN(walletName: String)
    fun restartApplication()
    fun hasInternetConnection(): Boolean
    fun getDataConsent(): Boolean
    fun setDataConsent(value: Boolean)
    fun saveGetInfoHtml(html: String)
    fun savePldLogHtml(html: String)
    fun getWalletInfoUrl(): String
    fun getPldLogUrl(): String
    fun setShowInactiveServers(value: Boolean)
    fun getShowInactiveServers(): Boolean
}