package com.pkt.domain.repository

import com.pkt.domain.dto.CjdnsConnection
import com.pkt.domain.dto.CjdnsInfo
import com.pkt.domain.dto.CjdnsPeer

interface CjdnsRepository {
    suspend fun getCjdnsRoutes() : Boolean
    suspend fun getCjdnsInfo(): Result<CjdnsInfo>
    suspend fun getCjdnsPeers(): List<CjdnsPeer>
    suspend fun getCjdnsConnections(): List<CjdnsConnection>
}