package com.odysee.app.updater

import com.odysee.app.core.data.updater.AppUpdater
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UpdaterModule {
    @Binds @Singleton abstract fun bindAppUpdater(impl: RemoteAppUpdater): AppUpdater
}
