package com.odysee.app.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.wallet.WalletBalance
import com.odysee.app.core.data.wallet.WalletRepository
import com.odysee.app.core.data.wallet.WalletTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalletUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val balance: WalletBalance? = null,
    val transactions: List<WalletTransaction> = emptyList(),
    val receiveAddress: String? = null,
    val isLoadingAddress: Boolean = false,
    val sendDraftAddress: String = "",
    val sendDraftAmount: String = "",
    val isSending: Boolean = false,
    val sendStatus: String? = null,
    val sendIsError: Boolean = false,
)

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WalletUiState())
    val state: StateFlow<WalletUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val balanceResult = runCatching { walletRepository.getBalance() }
            val txResult = runCatching { walletRepository.getTransactions() }
            val err = balanceResult.exceptionOrNull()?.message ?: txResult.exceptionOrNull()?.message
            _state.update {
                it.copy(
                    isLoading = false,
                    error = err,
                    balance = balanceResult.getOrNull(),
                    transactions = txResult.getOrNull().orEmpty(),
                )
            }
        }
    }

    fun ensureReceiveAddress() {
        if (_state.value.receiveAddress != null || _state.value.isLoadingAddress) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingAddress = true) }
            val result = runCatching { walletRepository.getReceiveAddress() }
            _state.update { st ->
                st.copy(
                    isLoadingAddress = false,
                    receiveAddress = result.getOrNull(),
                    error = result.exceptionOrNull()?.message ?: st.error,
                )
            }
        }
    }

    fun onSendDraftAddressChange(v: String) {
        _state.update { it.copy(sendDraftAddress = v, sendStatus = null) }
    }

    fun onSendDraftAmountChange(v: String) {
        _state.update {
            it.copy(
                sendDraftAmount = v.filter { ch -> ch.isDigit() || ch == '.' },
                sendStatus = null,
            )
        }
    }

    fun send() {
        val st = _state.value
        val address = st.sendDraftAddress.trim()
        val amount = st.sendDraftAmount.trim()
        if (address.isBlank()) {
            _state.update { it.copy(sendStatus = "Enter a destination address.", sendIsError = true) }
            return
        }
        val parsed = amount.toDoubleOrNull()
        if (parsed == null || parsed <= 0) {
            _state.update { it.copy(sendStatus = "Enter a positive amount.", sendIsError = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, sendStatus = null) }
            val result = runCatching { walletRepository.sendLbc(address, amount) }
            result.fold(
                onSuccess = { txid ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            sendStatus = "Sent! tx: ${txid.take(12)}…",
                            sendIsError = false,
                            sendDraftAddress = "",
                            sendDraftAmount = "",
                        )
                    }
                    load()
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            sendStatus = err.message ?: "Send failed.",
                            sendIsError = true,
                        )
                    }
                },
            )
        }
    }

    fun clearSendStatus() {
        _state.update { it.copy(sendStatus = null) }
    }
}
