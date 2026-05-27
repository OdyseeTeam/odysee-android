package com.odysee.app.flavor

import com.odysee.app.core.datastore.FlavorConfig
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FlavorConfigModule {
    @Binds
    @Singleton
    abstract fun bindFlavorConfig(impl: FlavorConfigImpl): FlavorConfig
}
