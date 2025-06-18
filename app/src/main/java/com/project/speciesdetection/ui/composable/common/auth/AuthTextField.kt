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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.R
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.ui.composable.common.CustomTextField
import com.project.speciesdetection.ui.composable.common.ErrorText

@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    leadingIcon: @Composable() (() -> Unit)? = null,
) {
    CustomTextField(

        value = value,
        leadingIcon = leadingIcon,

        onValueChange = onValueChange,
        placeholder = {
            Text(label) },
        supportingText = {
            if (error?.contains("empty") == true && value.isEmpty())
                ErrorText("$label "+ stringResource(R.string.empty_label_message))
            if (label == "Email" && error?.contains("formatted") == true)
                ErrorText(stringResource(R.string.bad_formated))
            if (label == "Email" && (error=="Email_already_registered_with_password"
                        || error=="Email_already_registered_with_google_or_password")) {
                ErrorText(stringResource(R.string.email_in_use))
            }

        },
        modifier = Modifier.fillMaxWidth().padding(),
        shape = MaterialTheme.shapes.extraLarge,

        isError = error?.contains("empty") == true && value.isEmpty()
                || error?.contains("formatted") == true && label.equals("email",true)
                || error?.contains("incorrect") == true
                || label == "Email" &&
                (error=="Email_already_registered_with_password"
                        || error=="Email_already_registered_with_google_or_password")
                || error?.contains("disabled") == true,
        paddingValues = 15.dp,

    )

}