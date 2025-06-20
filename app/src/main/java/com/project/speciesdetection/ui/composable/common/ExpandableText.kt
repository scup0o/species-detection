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
import androidx.compose.ui.text.TextLayoutResult
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
    var isExpanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val seeMore = " ...See More"
    val seeLess = " See Less"

    val annotatedText = buildAnnotatedString {
        if (isExpanded) {
            append(text)
            withStyle(style = SpanStyle(color = seeMoreColor, fontWeight = FontWeight.Bold)) {
                append(seeLess)
            }
        } else {
            // Chỉ thêm "See More" nếu thực sự bị tràn
            if (hasOverflow) {
                // Lấy vị trí cắt của dấu "..."
                val lastCharIndex = textLayoutResult?.getLineEnd(collapsedMaxLines - 1) ?: 0
                // Cắt văn bản gốc để chừa chỗ cho "... See More"
                // Cần tính toán cẩn thận để không bị cắt mất chữ
                val adjustedText = text.take(lastCharIndex)
                    .dropLast(seeMore.length) // Bỏ bớt ký tự để chừa chỗ
                    .dropLastWhile { it.isWhitespace() }

                append(adjustedText)
                withStyle(style = SpanStyle(color = seeMoreColor, fontWeight = FontWeight.Bold)) {
                    append(seeMore)
                }
            } else {
                append(text)
            }
        }
    }

    Box(modifier = modifier
        .animateContentSize()
        .clickable(enabled = hasOverflow) {
            isExpanded = !isExpanded
        }
    ) {
        Text(
            text = annotatedText,
            style = style,
            maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Clip, // Dùng Clip thay vì Ellipsis để chúng ta tự kiểm soát
            onTextLayout = { result ->
                // Chỉ gán lần đầu để xác định có bị tràn hay không
                if (textLayoutResult == null) {
                    hasOverflow = result.hasVisualOverflow
                    textLayoutResult = result
                }
            }
        )
    }
}