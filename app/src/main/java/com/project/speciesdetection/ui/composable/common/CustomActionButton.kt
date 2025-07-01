package com.project.speciesdetection.ui.composable.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.R

@Composable
fun CustomActionButton(
    text: String,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary,
    borderColor: Color = Color.Unspecified,
    vectorLeadingIcon: ImageVector? = null,
    painterLeadingIcon : Int? = null,
    contentColor : Color = Color.Unspecified,
    enabled : Boolean = true,

    ) {

    if (isLoading) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Button(
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            border = BorderStroke(
                width = if (borderColor.isUnspecified) 0.dp else 1.dp,
                color = borderColor,
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = contentColor
            ),
            enabled = enabled
        ) {
            if (vectorLeadingIcon!=null)
                Icon(vectorLeadingIcon, null)

            else{
                if (painterLeadingIcon!=null){
                    Image(
                        painterResource(painterLeadingIcon), null,
                        modifier = Modifier.size(24.dp),
                        colorFilter = null,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Text(text, modifier = Modifier.padding(8.dp))
        }
    }
}