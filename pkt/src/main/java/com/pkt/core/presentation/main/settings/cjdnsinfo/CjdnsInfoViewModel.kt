package com.pkt.core.presentation.main.settings.cjdnsinfo

import androidx.lifecycle.SavedStateHandle
import com.pkt.domain.dto.CjdnsInfo
import com.pkt.core.R
import com.pkt.core.di.qualifier.VersionName
import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.common.adapter.delegate.KeyValueHorizontalItem
import com.pkt.core.presentation.common.state.StateViewModel
import com.pkt.core.presentation.common.state.event.CommonEvent
import com.pkt.domain.repository.CjdnsRepository
import com.pkt.domain.repository.GeneralRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CjdnsInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @VersionName private val versionName: String,
    private val cjdnsRepository: CjdnsRepository,
    private val repository: GeneralRepository,
) : StateViewModel<CjdnsInfoState>() {

    init {
        invokeLoadingAction {
            loadCjdnsInfo()
        }
    }

    private suspend fun loadCjdnsInfo() : Result<*> {
        runCatching {
            cjdnsRepository.getCjdnsInfo().getOrThrow()
        }.onSuccess { info ->
            sendState {
                copy(
                    info = info,
                    items = info.toItems())
            }
        }.onFailure {
            sendError(it)
        }
        return Result.success(Unit)
    }

    override fun createInitialState() = CjdnsInfoState()

    fun onFindYourselfClick() {
        /*currentState.info?.nodeUrl?.let { url ->
            openWebUrl(url)
        }*/
    }

    fun onSubmitLogsClick() {
        if (repository.submitErrorLogs()) {
            sendEvent(CommonEvent.Info(R.string.logs_submitted))
        } else {
            sendEvent(CommonEvent.Warning(R.string.logs_submitted_consent))
        }
    }

    private fun CjdnsInfo.toItems(): List<DisplayableItem> {
        val info = this
        val connection = info.connection
        val peers = info.peers
        return mutableListOf<DisplayableItem>().apply {
            if (connection.key.isEmpty()) {//Not on VPN
                addAll(
                    listOf(
                        KeyValueHorizontalItem(R.string.app_version, versionName),
                        KeyValueHorizontalItem(R.string.ipv4, "Not on VPN"),
                        KeyValueHorizontalItem(R.string.internet_ipv6, "Not on VPN"),
                        KeyValueHorizontalItem(R.string.cjdns_ipv6, "Not on VPN"),
                        KeyValueHorizontalItem(R.string.public_key, info.key),
                    )
                )
            } else {
                addAll(
                    listOf(
                        KeyValueHorizontalItem(R.string.app_version, versionName),
                        KeyValueHorizontalItem(R.string.ipv4, info.ipv4),
                        KeyValueHorizontalItem(R.string.internet_ipv6, info.internetipv6),
                        KeyValueHorizontalItem(R.string.cjdns_ipv6, info.ipv6),
                        KeyValueHorizontalItem(R.string.public_key, info.key),
                        KeyValueClickableItem(R.string.node_map, R.string.find_your_self),
                    )
                )
            }

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
            if (connection.key.isEmpty()) {//Not on VPN
                addAll(
                    listOf(
                        TitleItem(R.string.connection),
                        KeyValueHorizontalItem(R.string.server, "Not on VPN"),
                        KeyValueHorizontalItem(
                            R.string.ipv4, "Not on VPN"
                        ),
                        KeyValueHorizontalItem(
                            R.string.ipv6, "Not on VPN"
                        ),
                    )
                )
            } else {
                addAll(
                    listOf(
                        TitleItem(R.string.connection),
                        KeyValueHorizontalItem(R.string.server, connection.key),
                        KeyValueHorizontalItem(
                            R.string.ipv4,
                            "${connection.ip4Address}/${connection.ip4Alloc}/${connection.ip4Prefix}"
                        ),
                        KeyValueHorizontalItem(
                            R.string.ipv6,
                            "${connection.ip6Address}/${connection.ip6Alloc}/${connection.ip6Prefix}"
                        ),
                    )
                )
            }

        }
    }

    private fun checkExpanded(id: String, default: Boolean = false) =
        (currentState.items.find { (it as? PeerItem)?.id == id } as? PeerItem)?.expanded ?: default
}
