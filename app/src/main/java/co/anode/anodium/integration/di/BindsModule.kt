package co.anode.anodium.integration.di

import co.anode.anodium.integration.presentation.navigation.FragmentNavigationHandler
import com.pkt.core.presentation.common.state.navigation.NavigationHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BindsModule {

    @Binds
    abstract fun bindsNavigationHandler(navigationHandler: FragmentNavigationHandler): NavigationHandler
}
