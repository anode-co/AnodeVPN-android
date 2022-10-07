package com.pkt.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class WalletInfo(
    val lightning: Lightning,
    val neutrino: Neutrino,
    val wallet: Wallet,
)

@Serializable
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
    val uris: List<String>,
    val version: String,
)

@Serializable
data class Neutrino(
    val bans: List<Ban>,
    val blockHash: String,
    val blockTimestamp: String,
    val height: Int,
    val isSyncing: Boolean,
    val peers: List<Peer>,
    val queries: List<Query>,
)

@Serializable
data class Wallet(
    val currentBlockHash: String,
    val currentBlockTimestamp: String,
    val currentHeight: Int,
    val walletStats: WalletStats,
    val walletVersion: Int,
)

@Serializable
data class Chain(
    val chain: String,
    val network: String,
)

@Serializable
data class Feature(
    val isKnown: Boolean,
    val isRequired: Boolean,
    val name: String,
)

@Serializable
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

@Serializable
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

@Serializable
data class Ban(
    val addr: String,
    val reason: String,
    val endTime: String,
    val banScore: Int,
)

/*@Serializable
data class ServerPeer(
    val feeFilter: Int,
    val peer: Peer,
    val connReq: ConnReq,
    val server: ChainService,
    val persistent: Boolean,
    val knownAddresses: HashMap<String, String>,
    val banMgr: BanMgr,
    val quit: String,
    recvSubscribers
    mtxSubscribers
)*/

@Serializable
data class Query(
    val peer: String,
    val command: String,
    val reqNum: Int,
    val createTime: Long,
    val lastRequestTime: Long,
    val lastResponseTime: Long,
)