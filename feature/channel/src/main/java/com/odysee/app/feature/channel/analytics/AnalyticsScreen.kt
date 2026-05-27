package com.odysee.app.feature.channel.analytics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odysee.app.core.data.analytics.ChannelStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Analytics") },
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }
        val stats = state.stats
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.error?.let { err ->
                Surface(
                    color = Color(0x33E2202D),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = err,
                        color = Color(0xFFE2202D),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            if (stats == null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No analytics yet — publish content and check back soon.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "Followers",
                    value = formatNumber(stats.subscribers),
                    trend = stats.subscriberChange,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Views",
                    value = formatNumber(stats.totalViews),
                    trend = stats.viewChange,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionHeader("Top performers")
            stats.topNewVideoUri?.takeIf { it.isNotBlank() }?.let { uri ->
                TopRow(
                    title = "Most-watched (recent)",
                    uri = uri,
                    value = "${formatNumber(stats.topNewVideoViews)} views",
                    trend = stats.topNewVideoViewChange,
                )
            }
            stats.topCommentedVideoUri?.takeIf { it.isNotBlank() }?.let { uri ->
                TopRow(
                    title = "Most-commented (recent)",
                    uri = uri,
                    value = "${formatNumber(stats.topCommentedVideoComments)} comments",
                    trend = stats.topCommentedVideoCommentChange,
                )
            }
            stats.topAllTimeVideoUri?.takeIf { it.isNotBlank() }?.let { uri ->
                TopRow(
                    title = "Most-watched (all time)",
                    uri = uri,
                    value = "${formatNumber(stats.topAllTimeVideoViews)} views",
                    trend = stats.topAllTimeVideoViewChange,
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, trend: Long, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.size(4.dp))
            TrendBadge(trend)
        }
    }
}

@Composable
private fun TopRow(title: String, uri: String, value: String, trend: Long) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = uri,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.width(8.dp))
                TrendBadge(trend)
            }
        }
    }
}

@Composable
private fun TrendBadge(trend: Long) {
    val color = when {
        trend > 0 -> Color(0xFF1AC04F)
        trend < 0 -> Color(0xFFE2202D)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val prefix = when {
        trend > 0 -> "+"
        else -> ""
    }
    Text(
        text = "${prefix}${trend}",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

private fun formatNumber(n: Long): String = when {
    n >= 1_000_000L -> "%.1fM".format(n / 1_000_000.0).removeSuffix(".0M") + (if (n % 1_000_000L == 0L) "M" else "")
    n >= 1_000L -> "%.1fK".format(n / 1_000.0).removeSuffix(".0K") + (if (n % 1_000L == 0L) "K" else "")
    else -> n.toString()
}
