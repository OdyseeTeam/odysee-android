package com.odysee.app.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odysee.app.core.network.LbryioApi
import com.odysee.app.core.network.dto.RewardDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RewardsState(
    val isLoading: Boolean = true,
    val rewards: List<RewardDto> = emptyList(),
    val claiming: Set<String> = emptySet(),
    val statusMessage: String? = null,
    val statusIsError: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val lbryioApi: LbryioApi,
) : ViewModel() {

    private val _state = MutableStateFlow(RewardsState())
    val state: StateFlow<RewardsState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = runCatching { lbryioApi.rewardList() }
            _state.update {
                it.copy(
                    isLoading = false,
                    rewards = result.getOrNull()?.data.orEmpty(),
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun claim(reward: RewardDto) {
        val code = reward.claimCode ?: return
        if (code in _state.value.claiming) return
        viewModelScope.launch {
            _state.update { it.copy(claiming = it.claiming + code, statusMessage = null) }
            val result = runCatching { lbryioApi.rewardClaim(code) }
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            claiming = it.claiming - code,
                            statusMessage = "Claimed ${reward.rewardAmount ?: 0.0} LBC.",
                            statusIsError = false,
                        )
                    }
                    load()
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            claiming = it.claiming - code,
                            statusMessage = err.message ?: "Couldn't claim reward.",
                            statusIsError = true,
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Rewards") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            return@Scaffold
        }
        if (state.rewards.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "No rewards available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = state.error ?: "Check back later — new rewards appear as you use Odysee.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = viewModel::load) { Text("Refresh") }
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.statusMessage?.let { msg ->
                item("status") {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.statusIsError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                    )
                }
            }
            items(state.rewards.size) { idx ->
                val reward = state.rewards[idx]
                val claimed = reward.transactionId != null
                val claiming = reward.claimCode != null && reward.claimCode in state.claiming
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reward.rewardTitle ?: rewardLabel(reward.rewardType),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        reward.rewardDescription?.takeIf { it.isNotBlank() }?.let { desc ->
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${reward.rewardAmount ?: 0.0} LBC",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    when {
                        claimed -> Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = "Claimed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        reward.claimCode != null -> Button(
                            onClick = { viewModel.claim(reward) },
                            enabled = !claiming,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(
                                text = if (claiming) "Claiming…" else "Claim",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        else -> Text(
                            text = "Locked",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun rewardLabel(type: String?): String = when (type) {
    "new_user" -> "New user"
    "new_android" -> "New Android user"
    "email_provided" -> "Confirm email"
    "new_channel" -> "First channel"
    "first_stream" -> "First stream"
    "first_publish" -> "First publish"
    "referrer" -> "Referral reward"
    "referee" -> "Referred user"
    "reward_code" -> "Reward code"
    "weekly_watch" -> "Weekly watch"
    "subscription" -> "Subscribe reward"
    "youtube_creator" -> "YouTube creator"
    null -> "Reward"
    else -> type
}
