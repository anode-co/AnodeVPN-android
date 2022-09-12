package com.pkt.dummy

import com.pkt.domain.dto.Addr
import com.pkt.domain.dto.CjdnsInfo
import com.pkt.domain.dto.WalletInfo
import com.pkt.dummy.dto.AddrDummy
import com.pkt.dummy.dto.CjdnsInfoDummy
import com.pkt.dummy.dto.WalletInfoDummy
import org.mapstruct.Mapper

@Mapper
interface AddrMapper {
    fun map(dummy: AddrDummy): Addr
}

@Mapper
interface WalletInfoMapper {
    fun map(dummy: WalletInfoDummy): WalletInfo
}

@Mapper
interface CjdnsInfoMapper {
    fun map(dummy: CjdnsInfoDummy): CjdnsInfo
}
