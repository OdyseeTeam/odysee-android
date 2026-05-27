package com.odysee.app.core.network.mapper

import com.odysee.app.core.model.Homepage
import com.odysee.app.core.model.HomepageCategory
import com.odysee.app.core.network.dto.HomepageCategoryDto
import com.odysee.app.core.network.dto.HomepageLangDto

fun HomepageLangDto.toDomain(): Homepage {
    val categories = categories
        .filterValues { it.channelIds.isNotEmpty() }
        .map { (id, dto) -> dto.toDomain(id) }
        .sortedBy { it.sortOrder }
    return Homepage(categories = categories)
}

private fun HomepageCategoryDto.toDomain(id: String): HomepageCategory = HomepageCategory(
    id = id,
    name = name,
    label = label,
    icon = icon,
    description = description,
    sortOrder = sortOrder ?: Int.MAX_VALUE,
    channelIds = channelIds,
    channelLimit = channelLimit?.toIntOrNull(),
    daysOfContent = daysOfContent,
    pageSize = pageSize ?: 12,
    excludeShorts = excludeShorts == true,
)
