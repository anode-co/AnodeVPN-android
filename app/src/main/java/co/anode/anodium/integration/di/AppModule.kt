package co.anode.anodium.integration.di

import android.content.Context
import co.anode.anodium.BuildConfig
import co.anode.anodium.integration.model.repository.WalletRepositoryImpl
import com.pkt.core.di.qualifier.VersionName
import com.pkt.domain.repository.VpnRepository
import com.pkt.domain.repository.WalletRepository
import com.pkt.dummy.repository.VpnRepositoryDummy
import com.pkt.dummy.repository.WalletRepositoryDummy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @VersionName
    @Provides
    fun providesVersionName(): String = BuildConfig.VERSION_NAME

    /*@Provides
    @Singleton
    fun providesWalletRepository(@ApplicationContext context: Context): WalletRepository =
        WalletRepositoryDummy(context)*/
    @Provides
    @Singleton
    fun providesWalletRepository(): WalletRepository = WalletRepositoryImpl()

    @Provides
    @Singleton
    fun providesVpnRepository(): VpnRepository = VpnRepositoryDummy()

//    @Provides
//    @Singleton
//    fun providesVpnRepository(): VpnRepository = VpnRepositoryImpl()
}
