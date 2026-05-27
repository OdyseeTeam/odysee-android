package com.odysee.app.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.model.Claim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchasesState(
    val items: List<Claim> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalItems: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PurchasesViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PurchasesState())
    val state: StateFlow<PurchasesState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { contentRepository.listPurchases(page = 1, pageSize = PAGE_SIZE) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = result.items,
                            page = result.page,
                            totalPages = result.totalPages,
                            totalItems = result.totalItems,
                        )
                    }
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoading = false, error = err.message ?: "Failed to load purchases") }
                }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore || s.page >= s.totalPages) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, error = null) }
            val next = s.page + 1
            runCatching { contentRepository.listPurchases(page = next, pageSize = PAGE_SIZE) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            items = it.items + result.items,
                            page = result.page,
                            totalPages = result.totalPages,
                            totalItems = result.totalItems,
                        )
                    }
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoadingMore = false, error = err.message ?: "Failed to load more") }
                }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private companion object { const val PAGE_SIZE = 10 }
}
