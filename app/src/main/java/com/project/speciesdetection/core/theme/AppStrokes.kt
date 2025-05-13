package com.project.speciesdetection.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AppStrokes(
    val none : Dp = 0.dp,
    val xs : Dp = 1.dp,
    val s : Dp = 2.dp,
    val m : Dp = 3.dp,
    val l : Dp = 4.dp,
    val xl : Dp = 5.dp
)

// Tạo một CompositionLocal để cung cấp AppSpacing
val LocalAppStrokes = staticCompositionLocalOf { AppStrokes() }

// Tạo một extension property cho MaterialTheme để dễ dàng truy cập
val androidx.compose.material3.MaterialTheme.strokes: AppStrokes
    @Composable
    get() = LocalAppStrokes.current