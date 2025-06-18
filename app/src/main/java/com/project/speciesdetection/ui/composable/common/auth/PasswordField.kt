package com.project.speciesdetection.ui.composable.common.auth

import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.R
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.ui.composable.common.CustomTextField
import com.project.speciesdetection.ui.composable.common.ErrorText

@Composable
fun PasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
    error: String?
) {


    CustomTextField(
        value = password,
        onValueChange = onPasswordChange,
        placeholder = {
            Text(
                stringResource(R.string.password_label),
            ) },
        supportingText = {
            if (error?.contains("empty") == true && password.isEmpty())
                ErrorText(stringResource(R.string.empty_password_message))
            if (error?.contains("incorrect") == true)
                ErrorText(stringResource(R.string.invalid_email_password_message))
            if (error?.contains("invalid")==true)
                ErrorText(stringResource(R.string.invalid_password_message))
            if (error?.contains("disabled")==true)
                ErrorText(stringResource(R.string.account_disabled_message))
        },
        visualTransformation =
            if (passwordVisible)
                VisualTransformation.None
            else PasswordVisualTransformation(),
        leadingIcon = {
            Icon(
                Icons.Default.Lock, null,

                modifier = Modifier.padding(horizontal = 15.dp),
            )
        },

        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    modifier = Modifier.padding(end = 20.dp),
                    painter =
                        if (passwordVisible)
                            painterResource(R.drawable.eye_open)
                        else painterResource(R.drawable.eye_closed),
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        },
        shape = MaterialTheme.shapes.extraLarge,

        modifier = Modifier.fillMaxWidth(),
        isError = error?.contains("empty") == true && password.isEmpty()
                || error?.contains("incorrect") == true
                || error?.contains("invalid")==true
                || error?.contains("disabled") == true,

        paddingValues = 15.dp,

    )
}