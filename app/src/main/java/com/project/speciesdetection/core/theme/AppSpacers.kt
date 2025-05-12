package com.project.speciesdetection.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AppSpacers(
    val none: Dp = 0.dp,
    val xxxs: Dp = 2.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val s: Dp = 12.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    val xxxl: Dp = 64.dp

    // val IconSizeSmall = 24.dp
    // val IconSizeMedium = 32.dp
    // val ButtonHeight = 48.dp
)

// Tạo một CompositionLocal để cung cấp AppSpacing
val LocalAppSpacing = staticCompositionLocalOf { AppSpacers() }

// Tạo một extension property cho MaterialTheme để dễ dàng truy cập
val androidx.compose.material3.MaterialTheme.spacing: AppSpacers
    @Composable
    get() = LocalAppSpacing.current