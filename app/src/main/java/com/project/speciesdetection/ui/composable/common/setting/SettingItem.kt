package com.project.speciesdetection.ui.composable.common.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.R
import com.project.speciesdetection.core.theme.spacing

@Composable
fun SettingItem(
    onClickAction : () -> Unit,
    vectorIcon : ImageVector? = null,
    painterIcon : Int? = null,
    title : Int,
    divider : Boolean = true
){
    Button(
        onClick = onClickAction,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape
    ){
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
            ) {
                if (vectorIcon!=null)
                    Icon(
                        vectorIcon,
                        null
                    )
                else
                    if (painterIcon!=null)
                        Icon(
                            painterResource(painterIcon),
                            null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )

                Text(
                    stringResource(title),
                    style = MaterialTheme.typography.bodyLarge
                )
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.AutoMirrored.Default.KeyboardArrowRight, null,
                    )
                }

            }

            if (divider) HorizontalDivider()
        }

    }
}