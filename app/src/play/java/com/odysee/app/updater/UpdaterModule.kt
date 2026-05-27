package com.odysee.app.updater

import com.odysee.app.core.data.updater.AppUpdater
import com.odysee.app.core.data.updater.NoOpAppUpdater
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UpdaterModule {
    @Provides @Singleton fun provideAppUpdater(): AppUpdater = NoOpAppUpdater()
}
