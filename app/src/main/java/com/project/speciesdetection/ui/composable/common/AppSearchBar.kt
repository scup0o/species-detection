package com.project.speciesdetection.ui.composable.common


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon // Material 3
import androidx.compose.material3.IconButton // Material 3
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme // Material 3
import androidx.compose.material3.Text // Material 3
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults // Material 3 <--- QUAN TRỌNG
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.core.theme.spacing

@Composable
fun AppSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearchAction: () -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "search",
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    cornerRadiusPercent: Int = 50,
    elevation: Dp = 5.dp
) {

    val color = MaterialTheme.colorScheme.primary

    CustomTextField(
        textStyle = MaterialTheme.typography.bodyMedium,
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier
            .graphicsLayer {
                shadowElevation = 1.dp.toPx()
                ambientShadowColor = color
                spotShadowColor = color
                clip = true
                shape = RoundedCornerShape(20.dp)
            }
            .heightIn(max = 56.dp)
            .background(backgroundColor),
        placeholder = {
            Text(
                hint,
                color = contentColor.copy(alpha = 0.7f),
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .clickable(onClick = onSearchAction)
                    .padding(MaterialTheme.spacing.xs)
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
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
                        tint = contentColor
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
        focusedTextColor = contentColor,
        unfocusedTextColor = contentColor,
        disabledTextColor = contentColor.copy(alpha = 0.5f),
        cursorColor = contentColor,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        shape = RoundedCornerShape(20.dp)
    )
}