package com.odysee.app.core.data.auth

import com.odysee.app.core.data.collections.FavoritesRepository
import com.odysee.app.core.data.collections.PlaylistsRepository
import com.odysee.app.core.data.collections.WatchLaterRepository
import com.odysee.app.core.data.history.WatchHistoryRepository
import com.odysee.app.core.data.moderation.BlockedChannelsRepository
import com.odysee.app.core.data.preferences.SharedPreferencesStore
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    suspend fun requestPasswordReset(email: String): SignInResult
    suspend fun requestAccountDeletion(): SignInResult
    suspend fun confirmEmail(email: String, verificationToken: String, authToken: String): SignInResult
    suspend fun checkPendingEmailVerification()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val lbryioApi: LbryioApi,
    private val sdkProxyApi: SdkProxyApi,
    private val authPreferences: AuthPreferences,
    private val authHolder: LbryioAuthHolder,
    private val sharedPreferencesStore: SharedPreferencesStore,
    private val subscriptionsRepository: Lazy<SubscriptionsRepository>,
    private val blockedChannelsRepository: Lazy<BlockedChannelsRepository>,
    private val watchLaterRepository: Lazy<WatchLaterRepository>,
    private val favoritesRepository: Lazy<FavoritesRepository>,
    private val playlistsRepository: Lazy<PlaylistsRepository>,
    private val tagsRepository: Lazy<TagsRepository>,
    private val watchHistoryRepository: Lazy<WatchHistoryRepository>,
    private val walletSyncRepository: Lazy<com.odysee.app.core.data.wallet.WalletSyncRepository>,
    private val clientSettingsRepository: Lazy<com.odysee.app.core.data.settings.ClientSettingsRepository>,
    private val crashReporter: CrashReporter,
) : AuthRepository {

    init {
        clientSettingsRepository.get().startAutoPush()
    }

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    override val state: StateFlow<AuthState> = _state.asStateFlow()
    private val bootstrapMutex = Mutex()
    private val hydrateMutex = Mutex()
    /** Token for which [hydrateSignedInSession] already finished this process. */
    @Volatile private var accountHydratedForToken: String? = null

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
                            hydrateSignedInSession()
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
        android.util.Log.i(TAG, "signIn: start hasPassword=${!password.isNullOrBlank()}")
        if (_state.value is AuthState.SignedIn) {
            runCatching { ensureAccountHydrated() }
            return SignInResult.Success
        }
        return runCatching { lbryioApi.userSignIn(email, password) }
            .fold(
                onSuccess = { env ->
                    if (env.success) {
                        adoptResponseToken(env.data?.authToken)
                        applyUserFromResponse(env.data)
                        if (_state.value !is AuthState.SignedIn) refreshUser()
                        if (password == null && _state.value !is AuthState.SignedIn) {
                            startVerificationPolling(email)
                            SignInResult.VerificationEmailSent
                        } else if (password != null && _state.value !is AuthState.SignedIn) {
                            SignInResult.Failure("Signed in, but the server hasn't confirmed your account yet. Try again.")
                        } else {
                            SignInResult.Success
                        }
                    } else {
                        resolveSignInApiError(env.error, email, passwordProvided = !password.isNullOrBlank())
                    }
                },
                onFailure = { error ->
                    if (error is HttpException) {
                        handleSignInHttpError(error, email, passwordProvided = !password.isNullOrBlank())
                    } else {
                        SignInResult.Failure(error.message ?: error::class.simpleName ?: "Sign-in failed")
                    }
                },
            ).also { result ->
                android.util.Log.i(TAG, "signIn: result=$result state=${_state.value::class.simpleName}")
            }
    }

    override suspend fun signUp(email: String, password: String?): SignInResult {
        return runCatching { lbryioApi.userSignUp(email, password) }
            .fold(
                onSuccess = { env ->
                    if (env.success) {
                        adoptResponseToken(env.data?.authToken)
                        applyUserFromResponse(env.data)
                        if (_state.value !is AuthState.SignedIn) refreshUser()
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

    private suspend fun applyUserFromResponse(dto: UserDto?) {
        val user = dto?.toDomain() ?: return
        val token = authHolder.get() ?: return
        _state.value = stateFor(token, user)
        if (user.email != null) authPreferences.setEmail(user.email)
        crashReporter.setUserId(user.id?.toString())
        if (_state.value is AuthState.SignedIn) {
            hydrateSignedInSession()
        }
    }

    override suspend fun signOut() {
        runCatching { lbryioApi.userSignout() }
        verificationJob?.cancel()
        verificationJob = null
        pendingVerificationEmail = null
        accountHydratedForToken = null
        authPreferences.setAuthToken(null)
        authPreferences.setEmail(null)
        authPreferences.setPendingVerificationEmail(null)
        authPreferences.setActiveChannelClaimId(null)
        authPreferences.setCustomPlaylists(null)
        authPreferences.setWatchLater(emptyList())
        authPreferences.setFavorites(emptyList())
        authPreferences.setSubscriptions(emptyList())
        authPreferences.setBlockedChannels(emptyList())
        authPreferences.setFollowedTags(emptyList())
        authPreferences.setWatchHistory(emptyList())
        runCatching { sharedPreferencesStore.invalidateCache() }
        authHolder.set(null)
        crashReporter.setUserId(null)
        _state.value = AuthState.Loading
        ensureBootstrap()
    }

    override suspend fun refreshUser() {
        syncAuthHolderFromStore()
        runCatching { lbryioApi.userMe() }
            .onSuccess { env ->
                val token = authHolder.get() ?: return@onSuccess
                val user = env.data?.toDomain() ?: return@onSuccess
                val wasSignedIn = _state.value is AuthState.SignedIn
                _state.value = stateFor(token, user)
                android.util.Log.i(
                    TAG,
                    "refreshUser: verified=${user.isEmailVerified} → ${_state.value::class.simpleName}",
                )
                if (user.email != null) authPreferences.setEmail(user.email)
                crashReporter.setUserId(user.id?.toString())
                if (_state.value is AuthState.SignedIn && !wasSignedIn) {
                    hydrateSignedInSession()
                }
            }
            .onFailure { android.util.Log.e(TAG, "refreshUser failed", it) }
    }

    private suspend fun syncAuthHolderFromStore() {
        val stored = authPreferences.authToken.firstOrNull()?.takeIf { it.isNotBlank() } ?: return
        if (authHolder.get() != stored) {
            android.util.Log.i(TAG, "authHolder re-synced from DataStore")
            authHolder.set(stored)
        }
    }

    override suspend fun selectActiveChannel(claimId: String) {
        val current = _state.value as? AuthState.SignedIn ?: return
        val channel = current.channels.firstOrNull { it.claimId == claimId } ?: return
        authPreferences.setActiveChannelClaimId(claimId)
        _state.value = current.copy(activeChannel = channel)
    }

    private suspend fun ensureAccountHydrated() {
        val signed = _state.value as? AuthState.SignedIn ?: return
        if (accountHydratedForToken == signed.authToken) return
        hydrateSignedInSession()
    }

    private suspend fun hydrateSignedInSession() {
        hydrateMutex.withLock {
            withContext(NonCancellable) {
                val token = (_state.value as? AuthState.SignedIn)?.authToken
                if (token != null && accountHydratedForToken == token) return@withContext
                android.util.Log.i(TAG, "hydrateSignedInSession: start")
                var channelsLoaded = 0
                for (attempt in 0 until HYDRATE_ATTEMPTS) {
                    if (attempt > 0) {
                        android.util.Log.i(TAG, "hydrateSignedInSession: retry #${attempt + 1}")
                        delay(1_000L * attempt)
                    }
                    runCatching { sharedPreferencesStore.invalidateCache() }
                    val syncResult = runCatching { walletSyncRepository.get().sync() }
                        .onFailure { android.util.Log.e(TAG, "wallet sync failed", it) }
                        .getOrNull()
                    android.util.Log.i(TAG, "hydrateSignedInSession: walletSync=$syncResult")
                    runCatching { sharedPreferencesStore.invalidateCache() }
                    channelsLoaded = refreshChannelList()
                    if (channelsLoaded > 0) break
                }
                syncAccountBoundSlices()
                accountHydratedForToken = token
                android.util.Log.i(
                    TAG,
                    "hydrateSignedInSession: done channels=$channelsLoaded",
                )
            }
        }
    }

    private suspend fun refreshChannelList(): Int {
        val items = runCatching {
            val response = sdkProxyApi.channelList(
                JsonRpcRequest(
                    method = "channel_list",
                    params = ChannelListParams(page = 1, pageSize = 99999, resolve = true),
                ),
            )
            val rpcError = response.error
            if (rpcError != null) error("channel_list: ${rpcError.message}")
            response.result?.items.orEmpty()
        }.onFailure { android.util.Log.e(TAG, "channel_list failed", it) }
            .getOrDefault(emptyList())

        android.util.Log.i(TAG, "channel_list: rawItems=${items.size}")
        val itemsByClaimId = items.associateBy { it.claimId }
        val channels = items
            .mapNotNull { it.toChannel() }
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
        return channels.size
    }

    private suspend fun syncAccountBoundSlices() {
        runCatching { subscriptionsRepository.get().syncFromServer() }
        runCatching { blockedChannelsRepository.get().syncFromServer() }
        runCatching { watchLaterRepository.get().syncFromServer() }
        runCatching { favoritesRepository.get().syncFromServer() }
        runCatching { playlistsRepository.get().syncFromServer() }
        runCatching { tagsRepository.get().syncFromServer() }
        runCatching { clientSettingsRepository.get().syncFromServer() }
        runCatching { watchHistoryRepository.get().syncFromServer() }
            .onFailure { android.util.Log.e(TAG, "watch history sync failed", it) }
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

    private suspend fun handleSignInHttpError(
        error: HttpException,
        email: String,
        passwordProvided: Boolean = true,
    ): SignInResult {
        val apiError = parseLbryioErrorBody(error)
        if (error.code() == 417) {
            runCatching { lbryioApi.resendEmailToken(email, onlyIfExpired = false) }
            startVerificationPolling(email)
            return SignInResult.TwoFactorRequired
        }
        return resolveSignInApiError(
            apiError
                ?: error.message().takeIf { it.isNotBlank() }
                ?: "Sign-in failed (HTTP ${error.code()})",
            email,
            passwordProvided = passwordProvided,
            httpCode = error.code(),
        )
    }

    private suspend fun resolveSignInApiError(
        apiError: String?,
        email: String,
        passwordProvided: Boolean = true,
        httpCode: Int? = null,
    ): SignInResult {
        val decision = SignInErrorClassifier.classify(apiError, passwordProvided, httpCode)
        android.util.Log.i(TAG, "resolveSignInApiError: decision=$decision passwordProvided=$passwordProvided")
        return when (decision) {
            SignInErrorClassifier.Decision.Promote -> {
                android.util.Log.i(TAG, "signIn: already-signed-in → promoting session")
                if (promoteAuthenticatedSession(email)) SignInResult.Success
                else SignInResult.Failure(apiError ?: "Couldn't restore session.")
            }
            SignInErrorClassifier.Decision.BadCredentials ->
                SignInResult.Failure(apiError?.takeIf { it.isNotBlank() } ?: "Incorrect email and/or password")
            SignInErrorClassifier.Decision.MagicLink -> {
                android.util.Log.i(TAG, "signIn: magic-link path → resend_token")
                runCatching { lbryioApi.resendEmailToken(email, onlyIfExpired = false) }
                startVerificationPolling(email)
                SignInResult.VerificationEmailSent
            }
            is SignInErrorClassifier.Decision.Failure -> SignInResult.Failure(decision.message)
        }
    }

    private suspend fun promoteAuthenticatedSession(emailHint: String? = null): Boolean {
        syncAuthHolderFromStore()
        refreshUser()
        val afterRefresh = _state.value
        if (afterRefresh is AuthState.SignedIn) {
            android.util.Log.i(TAG, "promoteAuthenticatedSession: user/me → SignedIn")
            runCatching { ensureAccountHydrated() }
            clearPendingVerification()
            return true
        }
        val token = authHolder.get()
            ?: authPreferences.authToken.firstOrNull()?.also { authHolder.set(it) }
        if (token.isNullOrBlank()) {
            android.util.Log.e(TAG, "promoteAuthenticatedSession: no auth token")
            return false
        }
        val email = emailHint?.takeIf { it.isNotBlank() }
            ?: pendingVerificationEmail
            ?: authPreferences.pendingVerificationEmail.firstOrNull()
            ?: authPreferences.email.firstOrNull()
        if (email.isNullOrBlank()) {
            android.util.Log.e(TAG, "promoteAuthenticatedSession: no email to attach")
            return false
        }
        android.util.Log.w(TAG, "promoteAuthenticatedSession: provisional SignedIn")
        _state.value = AuthState.SignedIn(
            authToken = token,
            user = User(
                id = null,
                email = email,
                isEmailVerified = true,
                isIdentityVerified = false,
                language = null,
            ),
        )
        clearPendingVerification()
        runCatching { ensureAccountHydrated() }
        // Confirm with user/me — don't keep a fake SignedIn if the token isn't verified.
        val confirmed = confirmProvisionalSignedIn(token)
        if (!confirmed) {
            android.util.Log.e(TAG, "promoteAuthenticatedSession: user/me did not confirm — reverting")
            _state.value = AuthState.Anonymous(token)
            accountHydratedForToken = null
        }
        return confirmed
    }

    private suspend fun confirmProvisionalSignedIn(token: String): Boolean {
        val env = runCatching { lbryioApi.userMe() }.getOrNull() ?: return false
        val user = env.data?.toDomain() ?: return false
        val channels = (_state.value as? AuthState.SignedIn)?.channels.orEmpty()
        val active = (_state.value as? AuthState.SignedIn)?.activeChannel
        val premium = (_state.value as? AuthState.SignedIn)?.premiumTier ?: PremiumTier.None
        val next = stateFor(token, user)
        if (next !is AuthState.SignedIn) return false
        _state.value = next.copy(
            channels = channels,
            activeChannel = active ?: channels.firstOrNull(),
            premiumTier = premium,
        )
        if (user.email != null) authPreferences.setEmail(user.email)
        crashReporter.setUserId(user.id?.toString())
        return true
    }

    private fun parseLbryioErrorBody(error: HttpException): String? {
        val raw = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        android.util.Log.w(TAG, "signIn http body length=${raw.length}")
        val match = Regex(""""error"\s*:\s*"((?:\\.|[^"\\])*)"""").find(raw) ?: return null
        return match.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .takeIf { it.isNotBlank() }
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
                    signOut()
                    SignInResult.Success
                } else {
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
    @Volatile private var pendingVerificationEmail: String? = null

    private fun startVerificationPolling(email: String) {
        pendingVerificationEmail = email
        verificationScope.launch {
            runCatching { authPreferences.setPendingVerificationEmail(email) }
        }
        verificationJob?.cancel()
        verificationJob = verificationScope.launch {
            android.util.Log.i(TAG, "verification poll: start")
            val deadline = System.currentTimeMillis() + 15 * 60_000L
            while (System.currentTimeMillis() < deadline) {
                delay(4_000)
                refreshUser()
                if (_state.value is AuthState.SignedIn) {
                    android.util.Log.i(TAG, "verification poll: SignedIn")
                    runCatching { ensureAccountHydrated() }
                    clearPendingVerification()
                    return@launch
                }
            }
            android.util.Log.w(TAG, "verification poll: timed out")
        }
    }

    private suspend fun clearPendingVerification() {
        pendingVerificationEmail = null
        verificationJob?.cancel()
        verificationJob = null
        authPreferences.setPendingVerificationEmail(null)
    }

    override suspend fun checkPendingEmailVerification() {
        val current = _state.value
        if (current is AuthState.SignedIn) {
            android.util.Log.i(TAG, "foreground check: already SignedIn")
            runCatching { ensureAccountHydrated() }
            return
        }
        if (pendingVerificationEmail == null) {
            pendingVerificationEmail = authPreferences.pendingVerificationEmail.firstOrNull()
        }
        android.util.Log.i(
            TAG,
            "foreground check: pending=${!pendingVerificationEmail.isNullOrBlank()} state=${_state.value::class.simpleName}",
        )
        repeat(3) { attempt ->
            refreshUser()
            if (_state.value is AuthState.SignedIn) {
                android.util.Log.i(TAG, "foreground check: SignedIn on attempt ${attempt + 1}")
                runCatching { ensureAccountHydrated() }
                clearPendingVerification()
                return
            }
            if (attempt < 2) delay(1_500)
        }
        val pending = pendingVerificationEmail
            ?: authPreferences.pendingVerificationEmail.firstOrNull()
        if (!pending.isNullOrBlank() && promoteAuthenticatedSession(pending)) {
            android.util.Log.i(TAG, "foreground check: promoted pending session")
            return
        }
        android.util.Log.w(TAG, "foreground check: still ${_state.value::class.simpleName}")
    }

    override suspend fun confirmEmail(
        email: String,
        verificationToken: String,
        authToken: String,
    ): SignInResult {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || verificationToken.isBlank() || authToken.isBlank()) {
            return SignInResult.Failure("Invalid verification link.")
        }

        if (pendingVerificationEmail == null) {
            pendingVerificationEmail = authPreferences.pendingVerificationEmail.firstOrNull()
        }
        val pending = pendingVerificationEmail
        if (pending.isNullOrBlank()) {
            return SignInResult.Failure(
                "Open the sign-in screen and request a link first, then tap the email again.",
            )
        }
        if (!pending.equals(cleanEmail, ignoreCase = true)) {
            return SignInResult.Failure(
                "This link is for a different email than the one you're signing in with.",
            )
        }

        val current = _state.value
        if (current is AuthState.SignedIn) {
            val currentEmail = current.user.email
            if (!currentEmail.isNullOrBlank() && currentEmail.equals(cleanEmail, ignoreCase = true)) {
                runCatching { ensureAccountHydrated() }
                clearPendingVerification()
                return SignInResult.Success
            }
            return SignInResult.Failure("You're signed in as another account. Sign out first.")
        }

        // Keep the previous session intact until the server accepts this link.
        val previousToken = authHolder.get()
            ?: authPreferences.authToken.firstOrNull()
        verificationJob?.cancel()
        verificationJob = null

        // Interceptor appends holder token into the form body; point it at the
        // link token only for this call, then restore on failure.
        authHolder.set(authToken)
        val result = runCatching {
            lbryioApi.userEmailConfirm(
                email = cleanEmail,
                verificationToken = verificationToken,
                authToken = authToken,
            )
        }
        return result.fold(
            onSuccess = { envelope ->
                val user = envelope.data?.toDomain()
                if (user == null) {
                    restoreAuthToken(previousToken)
                    SignInResult.Failure("Server returned no user after verification.")
                } else {
                    authPreferences.setAuthToken(authToken)
                    authHolder.set(authToken)
                    accountHydratedForToken = null
                    _state.value = stateFor(authToken, user)
                    if (user.email != null) authPreferences.setEmail(user.email)
                    clearPendingVerification()
                    if (_state.value is AuthState.SignedIn) hydrateSignedInSession()
                    SignInResult.Success
                }
            },
            onFailure = { err ->
                restoreAuthToken(previousToken)
                // Browser may have already consumed the link while upgrading
                // the *existing* device token — check without adopting link token.
                refreshUser()
                if (_state.value is AuthState.SignedIn) {
                    clearPendingVerification()
                    SignInResult.Success
                } else if (err is HttpException) {
                    handleSignInHttpError(err, cleanEmail, passwordProvided = false)
                } else {
                    SignInResult.Failure(err.message ?: "Verification failed.")
                }
            },
        )
    }

    private suspend fun restoreAuthToken(previousToken: String?) {
        authHolder.set(previousToken)
        // Do not write a blank token over a still-valid stored session.
        if (!previousToken.isNullOrBlank()) {
            authPreferences.setAuthToken(previousToken)
        }
    }

    private companion object {
        const val TAG = "AuthRepository"
        const val HYDRATE_ATTEMPTS = 5
    }
}

private fun UserDto.toDomain(): User = User(
    id = id,
    email = primaryEmail?.takeIf { it.isNotBlank() }
        ?: email?.takeIf { it.isNotBlank() },
    isEmailVerified = hasVerifiedEmail,
    isIdentityVerified = isIdentityVerified,
    language = language,
)
