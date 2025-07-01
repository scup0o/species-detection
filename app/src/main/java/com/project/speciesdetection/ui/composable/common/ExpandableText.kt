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

private const val DEFAULT_COLLAPSED_LINES = 3

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

    val seeMore = " "+stringResource(R.string.see_more)
    val seeLess = " "+stringResource(R.string.see_less)

    val annotatedText = buildAnnotatedString {
        if (isExpanded) {
            append(text)
            withStyle(style = SpanStyle(color = seeMoreColor, fontWeight = FontWeight.Bold)) {
                append(seeLess)
            }
        } else {
            if (hasOverflow) {
                val lastCharIndex = textLayoutResult?.getLineEnd(collapsedMaxLines - 1) ?: 0
                val adjustedText = text.take(lastCharIndex)
                    .dropLast(seeMore.length)
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
            overflow = TextOverflow.Clip,
            onTextLayout = { result ->
                if (textLayoutResult == null) {
                    hasOverflow = result.hasVisualOverflow
                    textLayoutResult = result
                }
            }
        )
    }
}