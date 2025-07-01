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

enum class PermissionStatus {
    Granted,
    Denied,
    Unknown
}

@Composable
fun NotificationPermissionEffect(
    key: Any?,
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) {
            onPermissionGranted()
        }
        return
    }

    val context = LocalContext.current
    var permissionStatus by remember { mutableStateOf(PermissionStatus.Unknown) }

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

    LaunchedEffect(key) {
        val currentStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )

        if (currentStatus == PackageManager.PERMISSION_GRANTED) {
            permissionStatus = PermissionStatus.Granted
            onPermissionGranted()
        } else {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}