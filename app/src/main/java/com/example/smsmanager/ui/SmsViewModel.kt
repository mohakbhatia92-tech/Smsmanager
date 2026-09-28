package com.example.smsmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsmanager.data.SenderStat
import com.example.smsmanager.data.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SmsUiState {
    object Loading : SmsUiState()
    data class Success(val senders: List<SenderStat>) : SmsUiState()
    data class Error(val message: String) : SmsUiState()
}

class SmsViewModel(private val repository: SmsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<SmsUiState>(SmsUiState.Loading)
    val uiState: StateFlow<SmsUiState> = _uiState

    fun loadData(days: Int? = 30) {
        viewModelScope.launch {
            _uiState.value = SmsUiState.Loading
            try {
                val stats = repository.getFrequentSenders(days)
                _uiState.value = SmsUiState.Success(stats)
            } catch (e: Exception) {
                _uiState.value = SmsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
            loadData()
        }
    }
    fun deleteSender(address: String) {
        viewModelScope.launch {
            repository.deleteMessagesBySender(address)
            loadData()
        }
    }
}