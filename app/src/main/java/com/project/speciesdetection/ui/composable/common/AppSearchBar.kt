package com.project.speciesdetection.ui.composable.common


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon // Material 3
import androidx.compose.material3.IconButton // Material 3
import androidx.compose.material3.MaterialTheme // Material 3
import androidx.compose.material3.Text // Material 3
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults // Material 3 <--- QUAN TRỌNG
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.core.theme.spacing

@OptIn(ExperimentalMaterial3Api::class) // Cần thiết cho TextFieldDefaults.colors
@Composable
fun AppSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearchAction: () -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "search",
    // Thêm các tham số tùy chỉnh màu sắc và hình dạng nếu cần
    backgroundColor: Color = MaterialTheme.colorScheme.surface, // Màu nền tùy chỉnh (điều chỉnh alpha nếu cần)
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant, // Màu chữ và icon
    cornerRadiusPercent: Int = 50, // 50% để tạo hình viên thuốc,
    elevation: Dp = 5.dp
) {

    TextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .shadow( // << THÊM MODIFIER SHADOW Ở ĐÂY
                elevation = elevation,
                shape = RoundedCornerShape(percent = cornerRadiusPercent), // Shadow sẽ theo hình dạng này
                clip = false, // Để shadow không bị cắt bởi clip của TextField bên dưới
                ambientColor = MaterialTheme.colorScheme.surfaceVariant,
                spotColor = MaterialTheme.colorScheme.outlineVariant
            )
            .heightIn(min = 52.dp) // Đặt chiều cao tối thiểu, Figma thường dùng 56dp hoặc tương tự
            .clip(RoundedCornerShape(percent = cornerRadiusPercent)) // Bo tròn góc
            .background(backgroundColor), // Đặt màu nền cho vùng TextField
        placeholder = {
            Text(hint, color = contentColor.copy(alpha = 0.7f), fontStyle = FontStyle.Italic)
                      }, // Placeholder text
        leadingIcon = {
            Box(
                modifier = Modifier
                    .clickable(onClick = onSearchAction)
                    .padding(MaterialTheme.spacing.m)
            ){
                Icon(
                    Icons.Filled.Search,
                    contentDescription =null,
                    tint = contentColor,
                )
            }

        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = {
                    onClearQuery()
                }) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = null,
                        tint = contentColor // Màu cho icon
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearchAction()
            }
        ),
        colors = TextFieldDefaults.colors( // Sử dụng TextFieldDefaults.colors
            focusedTextColor = contentColor,
            unfocusedTextColor = contentColor,
            disabledTextColor = contentColor.copy(alpha = 0.5f),
            cursorColor = contentColor,
            focusedIndicatorColor = Color.Transparent, // Loại bỏ đường gạch chân khi focus
            unfocusedIndicatorColor = Color.Transparent, // Loại bỏ đường gạch chân khi không focus
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = Color.Transparent, // Màu nền container của TextField, chúng ta đã dùng .background() bên ngoài
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            // Thêm các tùy chỉnh khác nếu cần
        ),
        shape = MaterialTheme.shapes.large
    )
}