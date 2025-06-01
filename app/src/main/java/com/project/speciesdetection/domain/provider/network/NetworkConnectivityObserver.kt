package com.project.speciesdetection.domain.provider.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log // Sử dụng Log của Android
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

// Implementation của ConnectivityObserver sử dụng ConnectivityManager của Android
class NetworkConnectivityObserver @Inject constructor(
    private val connectivityManager: ConnectivityManager
) : ConnectivityObserver {

    companion object {
        private const val TAG = "NetworkConnObserver" // Tag cho Log
    }

    override fun observe(): Flow<ConnectivityObserver.Status> {
        return callbackFlow {
            Log.d(TAG, "Starting network observation flow.")

            // Callback để lắng nghe các thay đổi trạng thái mạng
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.i(TAG, "Network available: $network. Checking for validated internet.")
                    // Kiểm tra thêm xem mạng có thực sự kết nối internet không
                    val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                    if (networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        Log.i(TAG, "Network $network has validated internet. Status: Available")
                        launch { send(ConnectivityObserver.Status.Available) }
                    } else {
                        // Có mạng nhưng chưa chắc có internet (ví dụ: wifi cần login)
                        // Tạm thời vẫn coi là Losing hoặc chờ onCapabilitiesChanged
                        Log.w(TAG, "Network $network available but not validated. Status might still be Losing/Unavailable.")
                        // launch { send(ConnectivityObserver.Status.Losing) } // Hoặc giữ trạng thái cũ
                    }
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    Log.w(TAG, "Network losing: $network, maxMsToLive: $maxMsToLive")
                    launch { send(ConnectivityObserver.Status.Losing) }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Log.e(TAG, "Network lost: $network. Checking overall status.")
                    // Khi một mạng bị mất, kiểm tra lại trạng thái tổng thể
                    // vì có thể vẫn còn mạng khác (ví dụ: mất wifi nhưng còn data di động)
                    val currentOverallStatus = getCurrentNetworkStatus()
                    Log.d(TAG, "After losing network $network, current overall status: $currentOverallStatus")
                    launch { send(currentOverallStatus) } // Gửi trạng thái tổng thể mới
                }

                override fun onUnavailable() {
                    // Được gọi khi không có mạng nào khả dụng lúc đăng ký callback
                    // (chỉ trên API level < 30, hoặc nếu không có mạng nào đáp ứng request ban đầu)
                    super.onUnavailable()
                    Log.e(TAG, "Network unavailable (onUnavailable callback).")
                    launch { send(ConnectivityObserver.Status.Unavailable) }
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    Log.d(TAG, "Network capabilities changed for $network: Validated=$isValidated, Internet=$hasInternet")
                    if (isValidated && hasInternet) {
                        launch { send(ConnectivityObserver.Status.Available) }
                    } else {
                        // Nếu mất validated internet, kiểm tra lại trạng thái tổng thể
                        val currentOverallStatus = getCurrentNetworkStatus()
                        launch { send(currentOverallStatus) }
                    }
                }
            }

            // Tạo request để theo dõi các mạng có khả năng kết nối internet
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // Muốn mạng có internet
                // .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) // Theo dõi cả mạng đã được xác thực
                .build()

            // Gửi trạng thái ban đầu khi flow bắt đầu observe
            val initialStatus = getCurrentNetworkStatus()
            Log.d(TAG, "Initial network status for observer: $initialStatus")
            trySend(initialStatus).isSuccess // Dùng trySend vì đang ở ngoài coroutine scope của launch

            // Đăng ký callback
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

            // Khi flow bị cancel (ví dụ ViewModel bị destroy), hủy đăng ký callback
            awaitClose {
                Log.d(TAG, "Stopping network observation, unregistering callback.")
                connectivityManager.unregisterNetworkCallback(networkCallback)
            }
        }.distinctUntilChanged() // Chỉ emit giá trị mới nếu trạng thái thực sự thay đổi
    }

    // Hàm private để lấy trạng thái mạng hiện tại một cách đồng bộ
    private fun getCurrentNetworkStatus(): ConnectivityObserver.Status {
        val activeNetwork = connectivityManager.activeNetwork // Lấy mạng đang hoạt động
        if (activeNetwork == null) {
            Log.d(TAG, "getCurrentNetworkStatus: No active network.")
            return ConnectivityObserver.Status.Unavailable
        }

        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (capabilities == null) {
            Log.d(TAG, "getCurrentNetworkStatus: Active network found, but no capabilities.")
            return ConnectivityObserver.Status.Unavailable // Hoặc Lost nếu đã từng có
        }

        // Kiểm tra xem mạng có kết nối internet thực sự không (VALIDATED)
        // và có một trong các loại transport phổ biến
        return if (
            (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && // Có khả năng truy cập internet
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)  // Kết nối internet đã được xác thực
        ) {
            Log.d(TAG, "getCurrentNetworkStatus: Network is Available (Validated Internet).")
            ConnectivityObserver.Status.Available
        } else if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            Log.w(TAG, "getCurrentNetworkStatus: Network has Internet capability but NOT Validated. Status: Losing/Unavailable.")
            // Có thể coi là Losing nếu đang chờ xác thực, hoặc Unavailable nếu không thể xác thực
            ConnectivityObserver.Status.Losing // Hoặc Unavailable tùy logic bạn muốn
        }
        else {
            Log.d(TAG, "getCurrentNetworkStatus: Network is Unavailable (No Validated Internet or suitable transport).")
            ConnectivityObserver.Status.Unavailable
        }
    }

    // Implement hàm lấy trạng thái hiện tại đồng bộ từ interface
    override fun getCurrentStatus(): ConnectivityObserver.Status {
        return getCurrentNetworkStatus()
    }
}