package com.project.speciesdetection.domain.provider.network

import kotlinx.coroutines.flow.Flow

// Interface để theo dõi trạng thái kết nối mạng
interface ConnectivityObserver {
    // Quan sát sự thay đổi trạng thái mạng dưới dạng một Flow
    fun observe(): Flow<Status>

    // Lấy trạng thái mạng hiện tại một cách đồng bộ
    fun getCurrentStatus(): Status

    // Các trạng thái mạng có thể có
    enum class Status {
        Available,    // Có mạng và có thể kết nối internet
        Unavailable,  // Không có kết nối mạng nào (ví dụ: Airplane mode, hoặc không có Wi-Fi/data)
        Losing,       // Đang trong quá trình mất kết nối
        Lost          // Đã mất kết nối (sau khi đã có)
    }
}