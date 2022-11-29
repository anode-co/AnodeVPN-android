package com.pkt.domain.repository

import com.pkt.domain.dto.CjdnsConnection
import com.pkt.domain.dto.CjdnsInfo
import com.pkt.domain.dto.CjdnsPeer
import com.pkt.domain.dto.CjdnsPeeringLine

interface CjdnsRepository {
    suspend fun getCjdnsRoutes() : Boolean
    suspend fun getCjdnsInfo(): Result<CjdnsInfo>
    suspend fun getCjdnsPeers(): Result<List<CjdnsPeer>>
    suspend fun getCjdnsConnections(): Result<List<CjdnsConnection>>
    suspend fun addCjdnsPeers(peers:List<CjdnsPeeringLine>): Boolean
    fun init()
}