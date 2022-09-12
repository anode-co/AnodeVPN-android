package com.pkt.core.presentation.main.settings.cjdnsinfo

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentCjdnsInfoBinding
import com.pkt.core.presentation.common.adapter.AsyncListDifferAdapter
import com.pkt.core.presentation.common.adapter.delegate.keyValueHorizontalAdapterDelegate
import com.pkt.core.presentation.common.state.StateFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CjdnsInfoFragment : StateFragment<CjdnsInfoState>(R.layout.fragment_cjdns_info) {

    private val viewBinding by viewBinding(FragmentCjdnsInfoBinding::bind)

    override val viewModel: CjdnsInfoViewModel by viewModels()

    private val adapter = AsyncListDifferAdapter(
        keyValueHorizontalAdapterDelegate(),
        keyValueClickableAdapterDelegate {
            viewModel.onFindYourselfClick()
        },
        titleAdapterDelegate(),
        peerAdapterDelegate(),
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            submitLogsButton.setOnClickListener {
                viewModel.onSubmitLogsClick()
            }

            recyclerView.itemAnimator = null
            recyclerView.adapter = adapter
        }
    }

    override fun handleState(state: CjdnsInfoState) {
        adapter.items = state.items
    }
}
