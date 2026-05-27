package com.odysee.app.core.data.notifications

import com.odysee.app.core.network.LbryioApi
import com.odysee.app.core.network.dto.NotificationDto
import com.odysee.app.core.network.dto.stringOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationItem(
    val id: Long,
    val rule: String,
    val title: String,
    val text: String,
    val target: String?,
    val channelUrl: String?,
    val channelThumbnail: String?,
    val isRead: Boolean,
    val isSeen: Boolean,
    val createdAt: String?,
)

interface NotificationsRepository {
    val unseenCount: StateFlow<Int>
    suspend fun list(): List<NotificationItem>
    suspend fun markRead(ids: List<Long>)
    suspend fun markSeen(ids: List<Long>)
    suspend fun refreshUnseen()
}

@Singleton
class NotificationsRepositoryImpl @Inject constructor(
    private val lbryioApi: LbryioApi,
) : NotificationsRepository {

    private val _unseenCount = MutableStateFlow(0)
    override val unseenCount: StateFlow<Int> = _unseenCount.asStateFlow()

    override suspend fun list(): List<NotificationItem> {
        val envelope = lbryioApi.notificationList()
        val items = envelope.data.orEmpty().map { it.toItem() }
        _unseenCount.value = items.count { !it.isSeen }
        return items
    }

    override suspend fun markRead(ids: List<Long>) {
        if (ids.isEmpty()) return
        lbryioApi.notificationEdit(notificationIds = ids.joinToString(","), isRead = true)
    }

    override suspend fun markSeen(ids: List<Long>) {
        if (ids.isEmpty()) return
        lbryioApi.notificationEdit(notificationIds = ids.joinToString(","), isSeen = true)
        _unseenCount.value = (_unseenCount.value - ids.size).coerceAtLeast(0)
    }

    override suspend fun refreshUnseen() {
        runCatching {
            val envelope = lbryioApi.notificationList()
            _unseenCount.value = envelope.data.orEmpty().count { !it.isSeen }
        }
    }
}

private fun NotificationDto.toItem(): NotificationItem {
    val device = parameters?.device
    val dynamic = parameters?.dynamic
    val channelUrl = dynamic?.stringOrNull("channel_url")
        ?: dynamic?.stringOrNull("comment_author")
        ?: dynamic?.stringOrNull("reply_author")
    val channelThumbnail = dynamic?.stringOrNull("channel_thumbnail")
        ?: dynamic?.stringOrNull("comment_author_thumbnail")
    return NotificationItem(
        id = id,
        rule = rule.orEmpty(),
        title = device?.title.orEmpty(),
        text = device?.text.orEmpty(),
        target = device?.target,
        channelUrl = channelUrl,
        channelThumbnail = channelThumbnail,
        isRead = isRead,
        isSeen = isSeen,
        createdAt = createdAt,
    )
}
