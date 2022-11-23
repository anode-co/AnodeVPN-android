package com.pkt.domain.repository

interface GeneralRepository {
    fun submitErrorLogs()
    fun enablePreReleaseUpgrade(value: Boolean)
    fun getPreReleaseUpgrade(): Boolean
    fun enableNewUI(value:Boolean)
    fun getNewUI():Boolean
    fun removePIN(walletName: String)
}