package com.pkt.core.presentation.main.settings.cjdnsinfo

import androidx.lifecycle.SavedStateHandle
import com.pkt.domain.dto.CjdnsInfo
import com.pkt.domain.repository.WalletRepository
import com.pkt.core.R
import com.pkt.core.di.qualifier.VersionName
import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.common.adapter.delegate.KeyValueHorizontalItem
import com.pkt.core.presentation.common.state.StateViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CjdnsInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @VersionName private val versionName: String,
    private val walletRepository: WalletRepository,
) : StateViewModel<CjdnsInfoState>() {

    private val address: String = savedStateHandle["address"] ?: throw IllegalArgumentException("address is required")

    init {
        invokeLoadingAction()
    }

    override fun createInitialState() = CjdnsInfoState()

    override fun createLoadingAction(): (suspend () -> Result<*>) = {
        walletRepository.getCjdnsInfo(address)
            .onSuccess { info ->
                sendState { copy(info = info, items = info.toItems()) }
            }
    }

    fun onFindYourselfClick() {
        currentState.info?.nodeUrl?.let { url ->
            openWebUrl(url)
        }
    }

    fun onSubmitLogsClick() {
        // TODO
    }

    private fun CjdnsInfo.toItems(): List<DisplayableItem> {
        val info = this
        val connection = info.connection
        val peers = info.peers
        return mutableListOf<DisplayableItem>().apply {
            addAll(
                listOf(
                    KeyValueHorizontalItem(R.string.app_version, versionName),
                    KeyValueHorizontalItem(R.string.ipv4, info.ipv4),
                    KeyValueHorizontalItem(R.string.internet_ipv6, info.internetipv6),
                    KeyValueHorizontalItem(R.string.cjdns_ipv6, connection.ip6Address),
                    KeyValueHorizontalItem(R.string.public_key, info.key),
                    KeyValueClickableItem(R.string.node_map, R.string.find_your_self),
                    KeyValueHorizontalItem(R.string.username, info.username),
                    KeyValueHorizontalItem(R.string.vpn_exit_key, info.vpnExit),
                )
            )

            if (peers.isNotEmpty()) {
                add(TitleItem(R.string.peers))

                addAll(
                    peers.mapIndexed { index, peer ->
                        PeerItem(peer, index < peers.size - 1).apply {
                            expanded = checkExpanded(id, index == 0)
                        }
                    }
                )
            }

            addAll(
                listOf(
                    TitleItem(R.string.connection),
                    KeyValueHorizontalItem(R.string.server, connection.key),
                    KeyValueHorizontalItem(R.string.ipv4,
                        "${connection.ip4Address}/${connection.ip4Alloc}/${connection.ip4Prefix}"),
                    KeyValueHorizontalItem(R.string.ipv6,
                        "${connection.ip6Address}/${connection.ip6Alloc}/${connection.ip6Prefix}"),
                )
            )
        }
    }

    private fun checkExpanded(id: String, default: Boolean = false) =
        (currentState.items.find { (it as? PeerItem)?.id == id } as? PeerItem)?.expanded ?: default
}
