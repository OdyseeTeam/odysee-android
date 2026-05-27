package com.odysee.app.cast

import com.odysee.app.core.data.cast.CastController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CastModule {
    @Binds
    @Singleton
    abstract fun bindCastController(impl: GoogleCastController): CastController
}
