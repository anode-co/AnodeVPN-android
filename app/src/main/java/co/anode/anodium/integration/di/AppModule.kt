package co.anode.anodium.integration.di

import co.anode.anodium.BuildConfig
import co.anode.anodium.integration.model.repository.CjdnsRepositoryImpl
import co.anode.anodium.integration.model.repository.VpnRepositoryImpl
import co.anode.anodium.integration.model.repository.WalletRepositoryImpl
import com.pkt.core.di.qualifier.VersionName
import com.pkt.domain.repository.CjdnsRepository
import com.pkt.domain.repository.VpnRepository
import com.pkt.domain.repository.WalletRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @VersionName
    @Provides
    fun providesVersionName(): String = BuildConfig.VERSION_NAME

    @Provides
    @Singleton
    fun providesWalletRepository(): WalletRepository = WalletRepositoryImpl()

    @Provides
    @Singleton
    fun providesVpnRepository(): VpnRepository = VpnRepositoryImpl()

    @Provides
    @Singleton
    fun providesCjdnsRepository(): CjdnsRepository = CjdnsRepositoryImpl()
}
