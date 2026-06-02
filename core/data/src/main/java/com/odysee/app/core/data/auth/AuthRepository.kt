package com.odysee.app.core.data.auth

import com.odysee.app.core.data.collections.FavoritesRepository
import com.odysee.app.core.data.collections.PlaylistsRepository
import com.odysee.app.core.data.collections.WatchLaterRepository
import com.odysee.app.core.data.moderation.BlockedChannelsRepository
import com.odysee.app.core.data.subscriptions.SubscriptionsRepository
import com.odysee.app.core.data.tags.TagsRepository
import com.odysee.app.core.common.telemetry.CrashReporter
import com.odysee.app.core.datastore.AuthPreferences
import com.odysee.app.core.model.Channel
import com.odysee.app.core.model.User
import com.odysee.app.core.network.LbryioApi
import com.odysee.app.core.network.LbryioAuthHolder
import com.odysee.app.core.network.SdkProxyApi
import com.odysee.app.core.network.dto.ChannelListParams
import com.odysee.app.core.network.dto.UserDto
import com.odysee.app.core.network.jsonrpc.JsonRpcRequest
import com.odysee.app.core.network.mapper.toChannel
import dagger.Lazy
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthState {
    data object Loading : AuthState
    data class Anonymous(val authToken: String) : AuthState
    data class SignedIn(
        val authToken: String,
        val user: User,
        val channels: List<Channel> = emptyList(),
        val activeChannel: Channel? = null,
        val premiumTier: PremiumTier = PremiumTier.None,
    ) : AuthState
    data class Error(val message: String) : AuthState
}

enum class PremiumTier { None, Premium, PremiumPlus }

sealed interface SignInResult {
    data object Success : SignInResult
    data object VerificationEmailSent : SignInResult
    data object TwoFactorRequired : SignInResult
    data class Failure(val message: String) : SignInResult
}

interface AuthRepository {
    val state: StateFlow<AuthState>
    suspend fun ensureBootstrap()
    suspend fun signIn(email: String, password: String? = null): SignInResult
    suspend fun signUp(email: String, password: String? = null): SignInResult
    suspend fun signOut()
    suspend fun refreshUser()
    suspend fun selectActiveChannel(claimId: String)
    /** Triggers an email with a reset link. The user follows that to set a new password. */
    suspend fun requestPasswordReset(email: String): SignInResult
    /** Sends an account deletion request to the server and signs out locally on success. */
    suspend fun requestAccountDeletion(): SignInResult
    /**
     * Completes email verification using the parameters from the verify deep link
     * ({@code odysee.com/$/verify?email=...&verification_token=...&auth_token=...}).
     */
    suspend fun confirmEmail(email: String, verificationToken: String, authToken: String): SignInResult
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val lbryioApi: LbryioApi,
    private val sdkProxyApi: SdkProxyApi,
    private val authPreferences: AuthPreferences,
    private val authHolder: LbryioAuthHolder,
    private val subscriptionsRepository: Lazy<SubscriptionsRepository>,
    private val blockedChannelsRepository: Lazy<BlockedChannelsRepository>,
    private val watchLaterRepository: Lazy<WatchLaterRepository>,
    private val favoritesRepository: Lazy<FavoritesRepository>,
    private val playlistsRepository: Lazy<PlaylistsRepository>,
    private val tagsRepository: Lazy<TagsRepository>,
    private val walletSyncRepository: Lazy<com.odysee.app.core.data.wallet.WalletSyncRepository>,
    private val crashReporter: CrashReporter,
) : AuthRepository {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    override val state: StateFlow<AuthState> = _state.asStateFlow()
    private val bootstrapMutex = Mutex()

    override suspend fun ensureBootstrap() = bootstrapMutex.withLock {
        val existing = authPreferences.authToken.firstOrNull()
        if (!existing.isNullOrBlank()) {
            authHolder.set(existing)
            runCatching { lbryioApi.userMe() }
                .onSuccess { env ->
                    val user = env.data?.toDomain()
                    if (user != null) {
                        _state.value = stateFor(existing, user)
                        if (user.email != null) authPreferences.setEmail(user.email)
                        crashReporter.setUserId(user.id?.toString())
                        if (_state.value is AuthState.SignedIn) {
                            loadChannels()
                            walletSyncRepository.get().sync()
                        }
                        return@withLock
                    }
                }
        }
        val appId = authPreferences.installationId()
        val env = lbryioApi.userNew(authToken = "", appId = appId, language = "en")
        val token = env.data?.authToken
            ?: error("user/new returned no auth_token")
        authPreferences.setAuthToken(token)
        authHolder.set(token)
        val user = env.data!!.toDomain()
        _state.value = stateFor(token, user)
        authPreferences.setEmail(user.email)
    }

    override suspend fun signIn(email: String, password: String?): SignInResult {
        return runCatching { lbryioApi.userSignIn(email, password) }
            .fold(
                onSuccess = { env ->
                    if (env.success) {
                        adoptResponseToken(env.data?.authToken)
                        refreshUser()
                        if (password == null && _state.value !is AuthState.SignedIn) {
                            startVerificationPolling(email)
                            SignInResult.VerificationEmailSent
                        } else {
                            SignInResult.Success
                        }
                    } else {
                        SignInResult.Failure(env.error ?: "Sign-in failed")
                    }
                },
                onFailure = { error ->
                    if (error is HttpException) handleSignInHttpError(error, email)
                    else SignInResult.Failure(error.message ?: error::class.simpleName ?: "Sign-in failed")
                },
            )
    }

    override suspend fun signUp(email: String, password: String?): SignInResult {
        return runCatching { lbryioApi.userSignUp(email, password) }
            .fold(
                onSuccess = { env ->
                    if (env.success) {
                        adoptResponseToken(env.data?.authToken)
                        refreshUser()
                        if (password != null) SignInResult.Success
                        else {
                            startVerificationPolling(email)
                            SignInResult.VerificationEmailSent
                        }
                    } else {
                        SignInResult.Failure(env.error ?: "Sign-up failed")
                    }
                },
                onFailure = { error ->
                    if (error is HttpException && error.code() == 409) {
                        signIn(email, password)
                    } else {
                        SignInResult.Failure(error.message ?: error::class.simpleName ?: "Sign-up failed")
                    }
                },
            )
    }

    override suspend fun signOut() {
        // Best-effort server-side sign-out; if the network call fails we still
        // tear down local state so the user appears signed out immediately.
        runCatching { lbryioApi.userSignout() }
        authPreferences.setAuthToken(null)
        authPreferences.setEmail(null)
        authPreferences.setActiveChannelClaimId(null)
        // Drop all account-bound local caches so a new sign-in starts clean and
        // we don't leak the previous user's playlists / subs / etc.
        authPreferences.setCustomPlaylists(null)
        authPreferences.setWatchLater(emptyList())
        authPreferences.setFavorites(emptyList())
        authPreferences.setSubscriptions(emptyList())
        authPreferences.setBlockedChannels(emptyList())
        authPreferences.setFollowedTags(emptyList())
        authHolder.set(null)
        crashReporter.setUserId(null)
        _state.value = AuthState.Loading
        ensureBootstrap()
    }

    override suspend fun refreshUser() {
        runCatching { lbryioApi.userMe() }
            .onSuccess { env ->
                val token = authHolder.get() ?: return@onSuccess
                val user = env.data?.toDomain() ?: return@onSuccess
                _state.value = stateFor(token, user)
                if (user.email != null) authPreferences.setEmail(user.email)
                crashReporter.setUserId(user.id?.toString())
                if (_state.value is AuthState.SignedIn) {
                    loadChannels()
                    walletSyncRepository.get().sync()
                }
            }
    }

    override suspend fun selectActiveChannel(claimId: String) {
        val current = _state.value as? AuthState.SignedIn ?: return
        val channel = current.channels.firstOrNull { it.claimId == claimId } ?: return
        authPreferences.setActiveChannelClaimId(claimId)
        _state.value = current.copy(activeChannel = channel)
    }

    private suspend fun loadChannels() {
        runCatching {
            val response = sdkProxyApi.channelList(
                JsonRpcRequest(method = "channel_list", params = ChannelListParams()),
            )
            response.result?.items.orEmpty()
        }.onSuccess { items ->
            val itemsByClaimId = items.associateBy { it.claimId }
            val channels = items
                .map { it.toChannel() }
                .sortedByDescending { ch ->
                    itemsByClaimId[ch.claimId]?.meta?.effectiveAmount?.toDoubleOrNull() ?: 0.0
                }
            val persistedId = authPreferences.activeChannelClaimId.firstOrNull()
            val active = channels.firstOrNull { it.claimId == persistedId }
                ?: channels.firstOrNull()
            val current = _state.value
            if (current is AuthState.SignedIn) {
                _state.value = current.copy(channels = channels, activeChannel = active)
            }
        }
        runCatching { subscriptionsRepository.get().syncFromServer() }
        runCatching { blockedChannelsRepository.get().syncFromServer() }
        runCatching { watchLaterRepository.get().syncFromServer() }
        runCatching { favoritesRepository.get().syncFromServer() }
        runCatching { playlistsRepository.get().syncFromServer() }
        runCatching { tagsRepository.get().syncFromServer() }
        runCatching { fetchPremiumStatus() }
    }

    private suspend fun fetchPremiumStatus() {
        val current = _state.value as? AuthState.SignedIn ?: return
        val claimIds = current.channels.map { it.claimId }
        if (claimIds.isEmpty()) {
            _state.update {
                if (it is AuthState.SignedIn) it.copy(premiumTier = PremiumTier.None) else it
            }
            return
        }
        runCatching { lbryioApi.userHasPremium(claimIds.joinToString(",")) }
            .onSuccess { env ->
                val byId = env.data ?: emptyMap()
                val hasPlus = byId.values.any { it.hasPremiumPlus }
                val hasPrem = byId.values.any { it.hasPremium }
                val tier = when {
                    hasPlus -> PremiumTier.PremiumPlus
                    hasPrem -> PremiumTier.Premium
                    else -> PremiumTier.None
                }
                _state.update {
                    if (it is AuthState.SignedIn) it.copy(premiumTier = tier) else it
                }
            }
    }

    private suspend fun handleSignInHttpError(error: HttpException, email: String): SignInResult {
        return when (error.code()) {
            409 -> {
                runCatching { lbryioApi.resendEmailToken(email, onlyIfExpired = true) }
                SignInResult.VerificationEmailSent
            }
            417 -> {
                runCatching { lbryioApi.resendEmailToken(email, onlyIfExpired = true) }
                SignInResult.TwoFactorRequired
            }
            else -> SignInResult.Failure(error.message() ?: "Sign-in failed (HTTP ${error.code()})")
        }
    }

    private fun stateFor(token: String, user: User): AuthState {
        return if (user.email != null && user.isEmailVerified) {
            AuthState.SignedIn(token, user)
        } else {
            AuthState.Anonymous(token)
        }
    }

    override suspend fun requestPasswordReset(email: String): SignInResult {
        if (email.isBlank()) return SignInResult.Failure("Enter your email.")
        return runCatching { lbryioApi.userPasswordReset(email.trim()) }.fold(
            onSuccess = { SignInResult.VerificationEmailSent },
            onFailure = { err ->
                SignInResult.Failure(err.message ?: "Couldn't request password reset.")
            },
        )
    }

    override suspend fun requestAccountDeletion(): SignInResult {
        return runCatching { lbryioApi.userDelete() }.fold(
            onSuccess = { env ->
                if (env.success) {
                    // Server accepted the deletion request. Sign out locally so the
                    // user can't keep using the app with a now-doomed account.
                    signOut()
                    SignInResult.Success
                } else {
                    // Server rejects deletion while the account still owns
                    // claims, has open transactions, etc. Surface the reason.
                    SignInResult.Failure(env.error ?: "Account can't be deleted right now.")
                }
            },
            onFailure = { err ->
                SignInResult.Failure(err.message ?: "Couldn't request account deletion.")
            },
        )
    }

    private suspend fun adoptResponseToken(token: String?) {
        val clean = token?.takeIf { it.isNotBlank() } ?: return
        authPreferences.setAuthToken(clean)
        authHolder.set(clean)
    }

    private val verificationScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default,
    )
    @Volatile private var verificationJob: kotlinx.coroutines.Job? = null

    private fun startVerificationPolling(email: String) {
        verificationJob?.cancel()
        verificationJob = verificationScope.launch {
            val deadline = System.currentTimeMillis() + 15 * 60_000L
            while (System.currentTimeMillis() < deadline) {
                delay(4_000)
                refreshUser()
                val signedIn = _state.value as? AuthState.SignedIn
                if (signedIn != null && signedIn.user.email.equals(email, ignoreCase = true)) {
                    return@launch
                }
            }
        }
    }

    override suspend fun confirmEmail(
        email: String,
        verificationToken: String,
        authToken: String,
    ): SignInResult {
        if (email.isBlank() || verificationToken.isBlank() || authToken.isBlank()) {
            return SignInResult.Failure("Invalid verification link.")
        }
        // The auth_token from the link becomes our new session token. Persist it
        // first so the subsequent call carries it, then ask the server to confirm.
        authPreferences.setAuthToken(authToken)
        authHolder.set(authToken)
        val result = runCatching {
            lbryioApi.userEmailConfirm(
                email = email.trim(),
                verificationToken = verificationToken,
                authToken = authToken,
            )
        }
        return result.fold(
            onSuccess = { envelope ->
                val user = envelope.data?.toDomain()
                if (user != null) {
                    _state.value = stateFor(authToken, user)
                    if (_state.value is AuthState.SignedIn) loadChannels()
                    SignInResult.Success
                } else {
                    SignInResult.Failure("Server returned no user after verification.")
                }
            },
            onFailure = { err ->
                if (err is HttpException) handleSignInHttpError(err, email)
                else SignInResult.Failure(err.message ?: "Verification failed.")
            },
        )
    }
}

private fun UserDto.toDomain(): User = User(
    id = id,
    email = primaryEmail?.takeIf { it.isNotBlank() },
    isEmailVerified = hasVerifiedEmail,
    isIdentityVerified = isIdentityVerified,
    language = language,
)
