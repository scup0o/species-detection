package com.project.speciesdetection.ui.composable.common.auth

import android.graphics.drawable.Icon
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.ui.composable.common.CustomTextField
import com.project.speciesdetection.ui.composable.common.ErrorText

@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
) {
    CustomTextField(

        value = value,
        leadingIcon = {
            if (label.equals("email",true))
                Icon(
                    Icons.Default.Email, null,
                    modifier = Modifier.padding(start = 20.dp)
                )
        },

        onValueChange = onValueChange,
        placeholder = {
            Text(label) },
        supportingText = {
            if (error?.contains("empty") == true && value.isEmpty())
                ErrorText("$label cannot be empty")
            if (label == "Email" && error?.contains("formatted") == true)
                ErrorText("Email is badly formatted")
            if (label == "Email" && (error=="Email_already_registered_with_password"
                        || error=="Email_already_registered_with_google_or_password")) {
                ErrorText("This email is already in use. Please use another.")
            }
        },
        modifier = Modifier.fillMaxWidth().padding(),
        shape = MaterialTheme.shapes.extraLarge,

        isError = error?.contains("empty") == true && value.isEmpty()
                || error?.contains("formatted") == true && label.equals("email",true)
                || error?.contains("incorrect") == true
                || label == "Email" &&
                (error=="Email_already_registered_with_password"
                        || error=="Email_already_registered_with_google_or_password"),
        paddingValues = 20.dp,

    )

}