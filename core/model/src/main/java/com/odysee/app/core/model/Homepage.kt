package com.odysee.app.core.model

data class Homepage(
    val categories: List<HomepageCategory>,
)

data class HomepageCategory(
    val id: String,
    val name: String,
    val label: String,
    val icon: String?,
    val description: String?,
    val sortOrder: Int,
    val channelIds: List<String>,
    val channelLimit: Int?,
    val daysOfContent: Int?,
    val pageSize: Int,
    val excludeShorts: Boolean,
    val languages: List<String> = emptyList(),
)
