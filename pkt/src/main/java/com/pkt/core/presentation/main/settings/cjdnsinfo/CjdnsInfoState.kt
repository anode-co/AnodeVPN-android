package com.pkt.core.presentation.main.settings.cjdnsinfo

import com.pkt.core.presentation.common.adapter.DisplayableItem
import com.pkt.core.presentation.common.state.UiState
import com.pkt.domain.dto.CjdnsInfo

data class CjdnsInfoState(
    val info: CjdnsInfo? = null,
    val items: List<DisplayableItem> = emptyList(),
) : UiState
