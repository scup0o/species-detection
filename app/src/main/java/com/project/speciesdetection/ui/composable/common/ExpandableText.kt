package com.project.speciesdetection.ui.composable.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.R // Giả sử bạn có string resource

// Các hằng số để code dễ đọc hơn
private const val DEFAULT_COLLAPSED_LINES = 3
const val SEE_MORE_TEXT = "... See More"
const val SEE_LESS_TEXT = " See Less"

@Composable
fun ExpandableText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle = LocalTextStyle.current,
    collapsedMaxLines: Int = DEFAULT_COLLAPSED_LINES,
    seeMoreColor: Color = MaterialTheme.colorScheme.primary
) {
    // State để theo dõi trạng thái mở rộng/thu gọn
    var isExpanded by remember { mutableStateOf(false) }
    // State để theo dõi xem văn bản có thực sự dài hơn số dòng cho phép không
    var isOverflowing by remember { mutableStateOf(false) }
    // State để lưu trữ văn bản sẽ được hiển thị (đã cắt hoặc đầy đủ)
    val textToShow = remember(isExpanded, isOverflowing) {
        buildAnnotatedString {
            if (isExpanded) {
                // Nếu mở rộng, hiển thị toàn bộ văn bản
                append(text)
                // Và thêm nút "Thu gọn"
                withStyle(
                    style = SpanStyle(
                        color = seeMoreColor,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(SEE_LESS_TEXT)
                }
            } else {
                // Nếu thu gọn, chỉ hiển thị phần văn bản đã được cắt bớt
                if (isOverflowing) {
                    // Nếu văn bản bị tràn, thêm nút "Xem thêm"
                    val seeMoreText = SEE_MORE_TEXT
                    // Lấy văn bản gốc và cắt bớt để chừa chỗ cho "... See More"
                    val cutText = text.take(
                        (text.length - seeMoreText.length).coerceAtLeast(0)
                    )
                    append(cutText)
                    withStyle(
                        style = SpanStyle(
                            color = seeMoreColor,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(seeMoreText)
                    }
                } else {
                    // Nếu không tràn, hiển thị toàn bộ văn bản
                    append(text)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .animateContentSize() // Thêm hiệu ứng animation mượt mà khi thay đổi kích thước
            .clickable(
                // Chỉ cho phép click khi văn bản bị tràn
                enabled = isOverflowing,
                onClick = { isExpanded = !isExpanded }
            )
    ) {
        Text(
            text = textToShow,
            maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis, // Cần thiết để onTextLayout hoạt động đúng
            style = style,
            onTextLayout = { textLayoutResult ->
                // Kiểm tra xem dòng cuối cùng có bị cắt bớt không.
                // Nếu có, tức là văn bản gốc dài hơn không gian hiển thị.
                if (!isExpanded && textLayoutResult.hasVisualOverflow) {
                    isOverflowing = true
                }
            }
        )
    }
}