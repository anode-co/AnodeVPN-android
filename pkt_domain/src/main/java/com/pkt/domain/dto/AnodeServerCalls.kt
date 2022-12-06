package com.pkt.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class RequestErrorPost(
    val publicKey: String = "",
    val error: String = "",
    val clientSoftwareVersion: String,
    val clientOs: String = "Android",
    val clientOsVersion: String = android.os.Build.VERSION.RELEASE,
    val localTimestamp: Long,
    val ip4Address: String = "",
    val ip6Address: String = "",
    val cpuUtilizationPercent: Int = 0,
    val availableMemoryBytes: Int = 0,
    val username: String = "",
    val message: String = "",
    val newAndroidLog: String = "",
    val previousAndroidLog: String = "",
    val debuggingMessages: String = ""
)

@Serializable
data class ResponseErrorPost(
    val status: String = "",
    val message: String = ""
)