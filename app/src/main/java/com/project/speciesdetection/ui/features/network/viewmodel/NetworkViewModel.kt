package com.project.speciesdetection.ui.features.network.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.domain.provider.network.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {
    private val _networkStatus = MutableStateFlow(ConnectivityObserver.Status.Unavailable) // Khởi tạo cẩn thận
    val networkStatus: StateFlow<ConnectivityObserver.Status> = _networkStatus.asStateFlow()

    init {
        viewModelScope.launch {
            _networkStatus.value = connectivityObserver.getCurrentStatus() // Lấy trạng thái mạng hiện tại ngay lập tức
            Log.d("network", "Initial network status set to: ${_networkStatus.value}")
            observeNetworkStatus() // Bắt đầu lắng nghe các thay đổi mạng

        }
    }

    // Lắng nghe các thay đổi trạng thái mạng
    private fun observeNetworkStatus() {
        viewModelScope.launch {
            connectivityObserver.observe()
                .distinctUntilChanged() // Chỉ xử lý khi trạng thái thực sự thay đổi
                .collect { status ->
                    val oldStatus = _networkStatus.value
                    _networkStatus.value = status
                    Log.i("network", "Network status changed: $oldStatus -> $status")

                }
        }
    }
}