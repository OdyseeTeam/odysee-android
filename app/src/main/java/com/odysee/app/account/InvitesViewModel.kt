package com.odysee.app.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.auth.AuthRepository
import com.odysee.app.core.data.auth.AuthState
import com.odysee.app.core.model.Channel
import com.odysee.app.core.network.LbryioApi
import com.odysee.app.core.network.dto.InviteeDto
import com.odysee.app.core.network.dto.RewardDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvitesState(
    val isLoading: Boolean = true,
    val invitesRemaining: Int = 0,
    val invitees: List<InviteeDto> = emptyList(),
    val referralCode: String = "",
    val channels: List<Channel> = emptyList(),
    val selectedReferralKey: String = "",
    val referralReward: RewardDto? = null,
    val isClaiming: Boolean = false,
    val isSending: Boolean = false,
    val emailDraft: String = "",
    val statusMessage: String? = null,
    val statusIsError: Boolean = false,
    val error: String? = null,
) {
    /** Web mirrors this: bare referral code, or {channelName}:{claimId} for a channel pick. */
    val referralString: String
        get() {
            val ch = channels.firstOrNull { it.name == selectedReferralKey }
            return if (ch != null) "${ch.name}:${ch.claimId}" else referralCode
        }
    val inviteUrl: String
        get() = "https://odysee.com/\$/invite/$referralString"
    val claimableReward: RewardDto?
        get() {
            val r = referralReward ?: return null
            // Only show claim button if there's an unclaimed claim_code AND an invitee
            // is claimable. Web's logic.
            val anyClaimable = invitees.any {
                it.inviteRewardClaimable == true && it.inviteRewardClaimed != true
            }
            return r.takeIf { anyClaimable && !r.claimCode.isNullOrBlank() }
        }
}

@HiltViewModel
class InvitesViewModel @Inject constructor(
    private val lbryioApi: LbryioApi,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(InvitesState())
    val state: StateFlow<InvitesState> = _state.asStateFlow()

    init {
        observeChannels()
        load()
    }

    private fun observeChannels() {
        val authState = authRepository.state.value
        val (channels, defaultKey) = if (authState is AuthState.SignedIn) {
            // Mirror web: default to "top" channel (most claims) for the referral.
            val top = authState.channels.firstOrNull()
            authState.channels to (top?.name ?: "")
        } else emptyList<Channel>() to ""
        _state.update { it.copy(channels = channels, selectedReferralKey = defaultKey) }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val statusResult = runCatching { lbryioApi.userInviteStatus() }
            val codeResult = runCatching { lbryioApi.userReferralCodeList() }
            val rewardResult = runCatching { lbryioApi.rewardList() }
            val status = statusResult.getOrNull()?.data
            val code = codeResult.getOrNull()?.data ?: ""
            val rewards = rewardResult.getOrNull()?.data.orEmpty()
            val referralReward = rewards.firstOrNull { it.rewardType == "referrer" }
            val state = _state.value
            // If the user has no explicit pick yet, fall back to the bare code now.
            val selected = if (state.selectedReferralKey.isBlank()) code else state.selectedReferralKey
            _state.update {
                it.copy(
                    isLoading = false,
                    invitesRemaining = status?.invitesRemaining ?: 0,
                    invitees = status?.invitees.orEmpty(),
                    referralCode = code,
                    referralReward = referralReward,
                    selectedReferralKey = selected,
                    error = if (status == null) statusResult.exceptionOrNull()?.message else null,
                )
            }
        }
    }

    fun selectReferralKey(key: String) {
        _state.update { it.copy(selectedReferralKey = key) }
    }

    fun onEmailDraftChange(value: String) {
        _state.update { it.copy(emailDraft = value, statusMessage = null) }
    }

    fun sendInvite() {
        val email = _state.value.emailDraft.trim()
        if (email.isBlank()) {
            _state.update { it.copy(statusMessage = "Enter an email first.", statusIsError = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, statusMessage = null) }
            val result = runCatching { lbryioApi.userInviteNew(email) }
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isSending = false,
                            emailDraft = "",
                            statusMessage = "Invite sent to $email.",
                            statusIsError = false,
                        )
                    }
                    load()
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            statusMessage = err.message ?: "Couldn't send invite.",
                            statusIsError = true,
                        )
                    }
                },
            )
        }
    }

    fun claimReward() {
        val code = _state.value.referralReward?.claimCode ?: return
        viewModelScope.launch {
            _state.update { it.copy(isClaiming = true, statusMessage = null) }
            val result = runCatching { lbryioApi.rewardClaim(code) }
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isClaiming = false,
                            statusMessage = "Invite credit claimed.",
                            statusIsError = false,
                        )
                    }
                    load()
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            isClaiming = false,
                            statusMessage = err.message ?: "Couldn't claim reward.",
                            statusIsError = true,
                        )
                    }
                },
            )
        }
    }
}
