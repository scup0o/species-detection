package com.project.speciesdetection.ui.composable.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

// Tạo một enum để theo dõi trạng thái quyền, giúp code dễ đọc hơn
enum class PermissionStatus {
    Granted,
    Denied,
    Unknown // Trạng thái ban đầu hoặc khi chưa cần kiểm tra
}

@Composable
fun NotificationPermissionEffect(
    // Truyền vào một key, ví dụ: userId. Khi key này thay đổi (người dùng đăng nhập),
    // effect sẽ chạy lại để kiểm tra quyền.
    key: Any?,
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
) {
    // Chỉ chạy logic này cho Android 13 (TIRAMISU) trở lên
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // Trên các phiên bản Android cũ, quyền được cấp tự động.
        // Gọi callback granted ngay lập tức.
        LaunchedEffect(Unit) {
            onPermissionGranted()
        }
        return
    }

    val context = LocalContext.current
    var permissionStatus by remember { mutableStateOf(PermissionStatus.Unknown) }

    // Launcher để yêu cầu quyền
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            permissionStatus = if (isGranted) {
                onPermissionGranted()
                PermissionStatus.Granted
            } else {
                onPermissionDenied()
                PermissionStatus.Denied
            }
        }
    )

    // Sử dụng LaunchedEffect để kiểm tra và yêu cầu quyền khi cần thiết
    LaunchedEffect(key) { // Effect sẽ chạy lại khi `key` thay đổi (vd: người dùng đăng nhập)
        val currentStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )

        if (currentStatus == PackageManager.PERMISSION_GRANTED) {
            permissionStatus = PermissionStatus.Granted
            onPermissionGranted()
        } else {
            // Nếu quyền chưa được cấp, chúng ta sẽ yêu cầu nó.
            // Có thể thêm logic hiển thị một dialog giải thích lý do tại sao
            // cần quyền trước khi gọi launcher.launch() để tăng tỷ lệ chấp nhận.
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}