package com.odysee.app.core.designsystem.comments

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun RichCommentBody(
    body: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    onHashtagClick: (String) -> Unit = {},
) {
    val segments = remember(body) { parseCommentSegments(body) }
    val linkColor = MaterialTheme.colorScheme.primary
    if (segments.all { it.sticker == null && it.emote == null }) {
        val annotated = remember(body, linkColor, onHashtagClick) {
            buildMarkdownAnnotated(body, linkColor, onHashtagClick)
        }
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = modifier,
        )
        return
    }
    CommentFlowRow(modifier = modifier.fillMaxWidth(), verticalSpacing = 2.dp) {
        segments.forEach { seg ->
            when {
                seg.sticker != null -> AsyncImage(
                    model = seg.sticker.url,
                    contentDescription = seg.sticker.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(96.dp).padding(end = 4.dp),
                )
                seg.emote != null -> AsyncImage(
                    model = seg.emote.url,
                    contentDescription = seg.emote.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(20.dp).padding(end = 2.dp),
                )
                !seg.text.isNullOrBlank() -> {
                    val annotated = remember(seg.text, linkColor, onHashtagClick) {
                        buildMarkdownAnnotated(seg.text, linkColor, onHashtagClick)
                    }
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentFlowRow(
    modifier: Modifier = Modifier,
    verticalSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val vSpacing = verticalSpacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val rows = mutableListOf<MutableList<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentWidth = 0
        placeables.forEach { p ->
            val nextWidth = currentWidth + p.width
            if (nextWidth > maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf(p)
                currentWidth = p.width
            } else {
                currentRow.add(p)
                currentWidth = nextWidth
            }
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)
        val height = rows.sumOf { row -> row.maxOf { it.height } } +
            (rows.size - 1).coerceAtLeast(0) * vSpacing
        layout(maxWidth, height) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                row.forEach { p ->
                    p.placeRelative(x, y)
                    x += p.width
                }
                y += row.maxOf { it.height } + vSpacing
            }
        }
    }
}

private val HASHTAG_REGEX_RICH = Regex("(?<![A-Za-z0-9_/])#([A-Za-z0-9_]{1,40})")

fun buildMarkdownAnnotated(
    text: String,
    linkColor: Color,
    onHashtagClick: (String) -> Unit = {},
): AnnotatedString = buildAnnotatedString {
    var i = 0
    val n = text.length
    fun matchSpan(start: Int, marker: String): Int {
        if (start + marker.length > n) return -1
        if (text.regionMatches(start, marker, 0, marker.length)) {
            val end = text.indexOf(marker, start + marker.length)
            if (end > 0 && end - (start + marker.length) > 0) return end
        }
        return -1
    }
    val urlRegex = Regex("(?i)https?://[^\\s]+")
    while (i < n) {
        val ch = text[i]
        val boldEnd = matchSpan(i, "**")
        if (boldEnd > 0) {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(text.substring(i + 2, boldEnd))
            pop()
            i = boldEnd + 2
            continue
        }
        val boldEnd2 = matchSpan(i, "__")
        if (boldEnd2 > 0) {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(text.substring(i + 2, boldEnd2))
            pop()
            i = boldEnd2 + 2
            continue
        }
        val strikeEnd = matchSpan(i, "~~")
        if (strikeEnd > 0) {
            pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
            append(text.substring(i + 2, strikeEnd))
            pop()
            i = strikeEnd + 2
            continue
        }
        if (ch == '*' && i + 1 < n && text[i + 1] != '*') {
            val close = text.indexOf('*', i + 1)
            if (close > i + 1) {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(text.substring(i + 1, close))
                pop()
                i = close + 1
                continue
            }
        }
        if (ch == '_' && i + 1 < n && text[i + 1] != '_') {
            val close = text.indexOf('_', i + 1)
            if (close > i + 1) {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(text.substring(i + 1, close))
                pop()
                i = close + 1
                continue
            }
        }
        if (ch == '`') {
            val close = text.indexOf('`', i + 1)
            if (close > i + 1) {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = linkColor.copy(alpha = 0.12f),
                    ),
                )
                append(text.substring(i + 1, close))
                pop()
                i = close + 1
                continue
            }
        }
        if (ch == '[') {
            val bracketEnd = text.indexOf(']', i + 1)
            if (bracketEnd > i + 1 && bracketEnd + 1 < n && text[bracketEnd + 1] == '(') {
                val urlEnd = text.indexOf(')', bracketEnd + 2)
                if (urlEnd > bracketEnd + 1) {
                    val label = text.substring(i + 1, bracketEnd)
                    val url = text.substring(bracketEnd + 2, urlEnd)
                    pushStringAnnotation(tag = "URL", annotation = url)
                    pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                    append(label)
                    pop()
                    pop()
                    i = urlEnd + 1
                    continue
                }
            }
        }
        val match = urlRegex.find(text, i)
        if (match != null && match.range.first == i) {
            val url = match.value
            val urlLink = androidx.compose.ui.text.LinkAnnotation.Url(
                url = url,
                styles = androidx.compose.ui.text.TextLinkStyles(
                    style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                ),
            )
            val idx = pushLink(urlLink)
            append(url)
            pop(idx)
            i += url.length
            continue
        }
        if (ch == '#') {
            val tagMatch = HASHTAG_REGEX_RICH.matchAt(text, i)
            if (tagMatch != null) {
                val raw = tagMatch.value
                val tag = tagMatch.groupValues[1]
                val link = androidx.compose.ui.text.LinkAnnotation.Clickable(
                    tag = "hashtag:$tag",
                    styles = androidx.compose.ui.text.TextLinkStyles(
                        style = SpanStyle(color = linkColor),
                    ),
                ) { onHashtagClick(tag) }
                val idx = pushLink(link)
                append(raw)
                pop(idx)
                i += raw.length
                continue
            }
        }
        append(ch)
        i += 1
    }
}
