package com.odysee.app.cast

import com.odysee.app.core.data.cast.CastController
import com.odysee.app.core.data.cast.NoOpCastController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CastModule {
    @Provides
    @Singleton
    fun provideCastController(): CastController = NoOpCastController()
}
