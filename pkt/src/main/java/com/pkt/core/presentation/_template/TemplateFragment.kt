package com.pkt.core.presentation._template

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentTemplateBinding
import com.pkt.core.presentation.common.state.StateFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TemplateFragment : StateFragment<TemplateState>(R.layout.fragment_template) {

    private val viewBinding by viewBinding(FragmentTemplateBinding::bind)

    override val viewModel: TemplateViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {}
    }

    override fun handleState(state: TemplateState) {}
}
