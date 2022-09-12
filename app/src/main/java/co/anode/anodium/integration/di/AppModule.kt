package co.anode.anodium.integration.di

import android.content.Context
import co.anode.anodium.BuildConfig
import com.pkt.core.di.qualifier.VersionName
import com.pkt.domain.repository.WalletRepository
import com.pkt.dummy.repository.WalletRepositoryDummy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @VersionName
    @Provides
    fun providesVersionName(): String = BuildConfig.VERSION_NAME

    @Provides
    fun providesWalletRepository(@ApplicationContext context: Context): WalletRepository =
        WalletRepositoryDummy(context)

//    @Provides
//    fun providesWalletRepository(): WalletRepository = WalletRepositoryImpl()
}
