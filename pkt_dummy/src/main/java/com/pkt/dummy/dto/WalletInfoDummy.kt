package com.pkt.dummy.dto

import kotlinx.serialization.Serializable

@Serializable
data class WalletInfoDummy(
    val lightning: LightningDummy,
    val neutrino: NeutrinoDummy,
    val wallet: WalletDummy,
)

@Serializable
data class LightningDummy(
    val alias: String,
    val bestHeaderTimestamp: String,
    val blockHash: String,
    val blockHeight: Int,
    val chains: List<ChainDummy>,
    val color: String,
    val commitHash: String,
    val features: Map<String, FeatureDummy>,
    val identityPubkey: String,
    val numActiveChannels: Int,
    val numInactiveChannels: Int,
    val numPeers: Int,
    val numPendingChannels: Int,
    val syncedToChain: Boolean,
    val syncedToGraph: Boolean,
    val testnet: Boolean,
    val version: String,
)

@Serializable
data class NeutrinoDummy(
    val blockHash: String,
    val blockTimestamp: String,
    val height: Int,
    val isSyncing: Boolean,
    val peers: List<PeerDummy>,
)

@Serializable
data class WalletDummy(
    val currentBlockHash: String,
    val currentBlockTimestamp: String,
    val currentHeight: Int,
    val walletStats: WalletStatsDummy,
    val walletVersion: Int,
)

@Serializable
data class ChainDummy(
    val chain: String,
    val network: String,
)

@Serializable
data class FeatureDummy(
    val isKnown: Boolean,
    val isRequired: Boolean,
    val name: String,
)

@Serializable
data class PeerDummy(
    val addr: String,
    val advertisedProtoVer: Int,
    val bytesReceived: String,
    val bytesSent: String,
    val connected: Boolean,
    val id: Int,
    val inbound: Boolean,
    val lastAnnouncedBlock: String?,
    val lastBlock: Int,
    val lastPingMicros: String,
    val lastPingNonce: String,
    val lastPingTime: String,
    val lastRecv: String,
    val lastSend: String,
    val na: String,
    val protocolVersion: Int,
    val sendHeadersPreferred: Boolean,
    val services: String,
    val startingHeight: Int,
    val timeConnected: String,
    val timeOffset: String,
    val userAgent: String,
    val verAckReceived: Boolean,
    val versionKnown: Boolean,
    val wireEncoding: String,
    val witnessEnabled: Boolean,
)

@Serializable
data class WalletStatsDummy(
    val birthdayBlock: Int,
    val maintenanceCycles: Int,
    val maintenanceInProgress: Boolean,
    val maintenanceLastBlockVisited: Int,
    val maintenanceName: String,
    val syncCurrentBlock: Int,
    val syncFrom: Int,
    val syncRemainingSeconds: String,
    val syncStarted: String,
    val syncTo: Int,
    val syncing: Boolean,
    val timeOfLastMaintenance: String,
)
