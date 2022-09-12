package com.pkt.core.presentation.main

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.view.View
import androidx.core.view.forEach
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.pkt.core.R
import com.pkt.core.databinding.FragmentMainBinding
import com.pkt.core.presentation.common.state.StateFragment
import com.pkt.core.presentation.common.state.state.CommonState
import com.pkt.core.util.color.LinearGradientSpan
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : StateFragment<CommonState.Empty>(R.layout.fragment_main) {

    private val viewBinding by viewBinding(FragmentMainBinding::bind)

    override val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            val navHostFragment = childFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
            val navController = navHostFragment.navController

            navController.addOnDestinationChangedListener { _, destination, _ ->
                bottomNavigationView.menu.forEach { item ->
                    val title = item.title.toString()
                    if (destination.hierarchy.any { it.id == item.itemId }) {
                        item.title = SpannableString(title).apply {
                            setSpan(LinearGradientSpan(title), 0, title.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                        }
                    } else {
                        item.title = title
                    }
                }
            }

            bottomNavigationView.apply {
                itemIconTintList = null
                setupWithNavController(navController)
            }
        }
    }
}
