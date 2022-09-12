package com.pkt.domain.dto

data class WalletInfo(
    val lightning: Lightning,
    val neutrino: Neutrino,
    val wallet: Wallet,
)

data class Lightning(
    val alias: String,
    val bestHeaderTimestamp: String,
    val blockHash: String,
    val blockHeight: Int,
    val chains: List<Chain>,
    val color: String,
    val commitHash: String,
    val features: Map<String, Feature>,
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

data class Neutrino(
    val blockHash: String,
    val blockTimestamp: String,
    val height: Int,
    val isSyncing: Boolean,
    val peers: List<Peer>,
)

data class Wallet(
    val currentBlockHash: String,
    val currentBlockTimestamp: String,
    val currentHeight: Int,
    val walletStats: WalletStats,
    val walletVersion: Int,
)

data class Chain(
    val chain: String,
    val network: String,
)

data class Feature(
    val isKnown: Boolean,
    val isRequired: Boolean,
    val name: String,
)

data class Peer(
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

data class WalletStats(
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
