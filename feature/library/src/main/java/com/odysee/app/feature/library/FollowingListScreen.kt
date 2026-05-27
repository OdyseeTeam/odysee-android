package com.odysee.app.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.subscriptions.Subscription
import com.odysee.app.core.data.subscriptions.SubscriptionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.serialization.Serializable

@Serializable
data object FollowingListRoute

data class FollowedChannel(
    val claimId: String,
    val handle: String,
    val title: String,
    val thumbnailUrl: String?,
)

@HiltViewModel
class FollowingListViewModel @Inject constructor(
    private val subscriptionsRepository: SubscriptionsRepository,
    private val contentRepository: ContentRepository,
) : ViewModel() {

    private val metadata = MutableStateFlow<Map<String, FollowedChannel>>(emptyMap())
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val subscriptions: StateFlow<List<Subscription>> = subscriptionsRepository.subscriptions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val items: StateFlow<List<FollowedChannel>> = combine(subscriptions, metadata, _query) { subs, meta, q ->
        val enriched = subs.map { sub ->
            meta[sub.claimId] ?: FollowedChannel(
                claimId = sub.claimId,
                handle = sub.name,
                title = sub.name.removePrefix("@"),
                thumbnailUrl = null,
            )
        }
        if (q.isBlank()) enriched else {
            val needle = q.trim()
            enriched.filter {
                it.title.contains(needle, ignoreCase = true) ||
                    it.handle.contains(needle, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            subscriptions.collect { subs ->
                val missing = subs.map { it.claimId }.filter { it !in metadata.value }
                if (missing.isEmpty()) return@collect
                val byId = subs.associateBy { it.claimId }
                missing.chunked(15).forEach { chunk ->
                    launch {
                        val channels = runCatching { contentRepository.getChannels(chunk) }
                            .getOrNull().orEmpty()
                        if (channels.isEmpty()) return@launch
                        metadata.update { current ->
                            val next = current.toMutableMap()
                            channels.forEach { ch ->
                                val sub = byId[ch.claimId]
                                next[ch.claimId] = FollowedChannel(
                                    claimId = ch.claimId,
                                    handle = sub?.name ?: ch.name,
                                    title = ch.title?.takeIf { it.isNotBlank() }
                                        ?: (sub?.name ?: ch.name).removePrefix("@"),
                                    thumbnailUrl = ch.thumbnailUrl,
                                )
                            }
                            next
                        }
                    }
                }
            }
        }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun unfollow(claimId: String) {
        viewModelScope.launch { subscriptionsRepository.unsubscribe(claimId) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingListScreen(
    viewModel: FollowingListViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onChannelClick: (String, String) -> Unit,
) {
    BackHandler(onBack = onBack)
    val items by viewModel.items.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val subs by viewModel.subscriptions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Following")
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = subs.size.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Filter following") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            if (items.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (query.isBlank()) "Not following anyone" else "No matches",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (query.isBlank()) {
                        Text(
                            text = "Tap Follow on a channel to add it to your list.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = padding.calculateBottomPadding() + 16.dp,
                    ),
                ) {
                    items(items, key = { it.claimId }) { ch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChannelClick(ch.claimId, ch.handle) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                val avatarUrl = ch.thumbnailUrl
                                if (!avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp).clip(CircleShape),
                                    )
                                } else {
                                    Text(
                                        text = (ch.title.firstOrNull { it.isLetterOrDigit() }
                                            ?: ch.handle.firstOrNull { it.isLetterOrDigit() }
                                            ?: '@').uppercaseChar().toString(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ch.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = ch.handle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.unfollow(ch.claimId) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            ) {
                                Text("Following", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
