package com.project.speciesdetection.ui.composable.common
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import android.util.Log

@Composable
fun HyperlinkText(
    fullText: String,
    linkText: String,
    url: String,
    hyperlinkStyle: SpanStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline
    ),
    normalTextStyle: SpanStyle = SpanStyle(color = MaterialTheme.colorScheme.onSurface) // Màu chữ bình thường
) {
    val uriHandler = LocalUriHandler.current
    val annotatedString = buildAnnotatedString {
        val startIndex = fullText.indexOf(linkText)
        val endIndex = startIndex + linkText.length

        // Phần văn bản trước link
        if (startIndex > 0) {
            withStyle(style = normalTextStyle) {
                append(fullText.substring(0, startIndex))
            }
        }

        // Phần link
        pushStringAnnotation(tag = "URL", annotation = url) // Đánh dấu phần này là URL
        withStyle(style = hyperlinkStyle) {
            append(linkText)
        }
        pop() // Kết thúc annotation

        // Phần văn bản sau link
        if (endIndex < fullText.length) {
            withStyle(style = normalTextStyle) {
                append(fullText.substring(endIndex))
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(color = normalTextStyle.color), // Áp dụng style chung
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item) // annotation.item chính là URL
                    } catch (e: Exception) {
                        Log.e("HyperlinkText", "Could not open URL: ${annotation.item}", e)
                    }
                }
        }
    )
}