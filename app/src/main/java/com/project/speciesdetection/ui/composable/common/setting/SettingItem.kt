package com.project.speciesdetection.ui.composable.common.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.project.speciesdetection.R
import com.project.speciesdetection.core.theme.spacing

@Composable
fun SettingItem(
    onClickAction : () -> Unit,
    vectorIcon : ImageVector? = null,
    painterIcon : Int? = null,
    title : Int
){
    Button(
        onClick = onClickAction
    ){
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
                        null
                    )

            Text(
                stringResource(title)
            )
        }
    }
}