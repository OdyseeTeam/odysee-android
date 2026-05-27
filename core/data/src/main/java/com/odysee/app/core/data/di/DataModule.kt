package com.odysee.app.core.data.di

import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.ContentRepositoryImpl
import com.odysee.app.core.data.auth.AuthRepository
import com.odysee.app.core.data.auth.AuthRepositoryImpl
import com.odysee.app.core.data.subscriptions.SubscriptionsRepository
import com.odysee.app.core.data.subscriptions.SubscriptionsRepositoryImpl
import com.odysee.app.core.data.collections.FavoritesRepository
import com.odysee.app.core.data.collections.FavoritesRepositoryImpl
import com.odysee.app.core.data.collections.WatchLaterRepository
import com.odysee.app.core.data.collections.WatchLaterRepositoryImpl
import com.odysee.app.core.data.history.WatchHistoryRepository
import com.odysee.app.core.data.history.WatchHistoryRepositoryImpl
import com.odysee.app.core.data.moderation.BlockedChannelsRepository
import com.odysee.app.core.data.moderation.BlockedChannelsRepositoryImpl
import com.odysee.app.core.data.reactions.ReactionsRepository
import com.odysee.app.core.data.reactions.ReactionsRepositoryImpl
import com.odysee.app.core.data.tags.TagsRepository
import com.odysee.app.core.data.tags.TagsRepositoryImpl
import com.odysee.app.core.data.notifications.NotificationsRepository
import com.odysee.app.core.data.notifications.NotificationsRepositoryImpl
import com.odysee.app.core.data.wallet.WalletRepository
import com.odysee.app.core.data.wallet.WalletRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionsRepository(impl: SubscriptionsRepositoryImpl): SubscriptionsRepository

    @Binds
    @Singleton
    abstract fun bindWalletRepository(impl: WalletRepositoryImpl): WalletRepository

    @Binds
    @Singleton
    abstract fun bindNotificationsRepository(impl: NotificationsRepositoryImpl): NotificationsRepository

    @Binds
    @Singleton
    abstract fun bindWatchHistoryRepository(impl: WatchHistoryRepositoryImpl): WatchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindWatchLaterRepository(impl: WatchLaterRepositoryImpl): WatchLaterRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistsRepository(
        impl: com.odysee.app.core.data.collections.PlaylistsRepositoryImpl,
    ): com.odysee.app.core.data.collections.PlaylistsRepository

    @Binds
    @Singleton
    abstract fun bindBlockedChannelsRepository(impl: BlockedChannelsRepositoryImpl): BlockedChannelsRepository

    @Binds
    @Singleton
    abstract fun bindReactionsRepository(impl: ReactionsRepositoryImpl): ReactionsRepository

    @Binds
    @Singleton
    abstract fun bindTagsRepository(impl: TagsRepositoryImpl): TagsRepository
}
