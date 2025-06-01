package com.project.speciesdetection.ui.composable.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider // Hoặc dùng Box nếu muốn tùy chỉnh màu dễ hơn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.speciesdetection.R

@Composable
fun Divider(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.login_divider),
    lineColor: Color = MaterialTheme.colorScheme.outlineVariant, // Màu đường kẻ
    lineThickness: Dp = 1.dp, // Độ dày đường kẻ
    textColor: Color = MaterialTheme.colorScheme.outline // Màu chữ
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(
            modifier = Modifier
                .weight(1f) // Để Divider chiếm không gian còn lại
                .height(lineThickness),
            color = lineColor
        )
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp), // Khoảng cách giữa chữ và đường kẻ
            color = textColor,
            fontSize = 14.sp // Bạn có thể điều chỉnh kích thước chữ
        )
        HorizontalDivider(
            modifier = Modifier
                .weight(1f) // Để Divider chiếm không gian còn lại
                .height(lineThickness),
            color = lineColor
        )
    }
}