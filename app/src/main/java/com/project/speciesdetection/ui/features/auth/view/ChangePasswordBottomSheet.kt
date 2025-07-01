package com.project.speciesdetection.ui.features.auth.view

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speciesdetection.R
import com.project.speciesdetection.ui.composable.common.CustomActionButton
import com.project.speciesdetection.ui.composable.common.ErrorText
import com.project.speciesdetection.ui.composable.common.auth.PasswordField
import com.project.speciesdetection.ui.features.auth.viewmodel.ChangePasswordUiEvent
import com.project.speciesdetection.ui.features.auth.viewmodel.ChangePasswordViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordBottomSheet(
    onDismissRequest: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // State cho các trường input
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // State cho việc hiển thị/ẩn mật khẩu
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val changePasswordSuccessMessage = stringResource(R.string.change_password_sucess)

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is ChangePasswordUiEvent.ShowToast -> {
                    val message =
                        when (event.message) {
                            "success" -> changePasswordSuccessMessage
                            else -> ""
                        }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onDismissRequest()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetState()
        }
    }

    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        modifier = Modifier
            .imePadding()
            .fillMaxSize()
            .padding(top = 50.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Default.KeyboardArrowLeft, null,
                    modifier = Modifier.clickable {
                        onDismissRequest()
                    }
                )
                Text(
                    text = stringResource(R.string.change_password),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

            }
            Spacer(modifier = Modifier.height(30.dp))

            PasswordField(
                password = currentPassword,
                onPasswordChange = { currentPassword = it },
                passwordVisible = currentPasswordVisible,
                onToggleVisibility = { currentPasswordVisible = !currentPasswordVisible },
                error = uiState.error,
                label = stringResource(R.string.change_password_current)
            )

            Spacer(modifier = Modifier.height(16.dp))

            PasswordField(
                password = newPassword,
                onPasswordChange = { newPassword = it },
                passwordVisible = newPasswordVisible,
                onToggleVisibility = { newPasswordVisible = !newPasswordVisible },
                error = uiState.error,
                label = stringResource(R.string.change_password_new)
            )

            Spacer(modifier = Modifier.height(16.dp))

            PasswordField(
                password = confirmPassword,
                onPasswordChange = { confirmPassword = it },
                passwordVisible = confirmPasswordVisible,
                onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                error = uiState.error,
                label = stringResource(R.string.change_password_confirm)
            )

            Spacer(modifier = Modifier.height(32.dp))

            CustomActionButton(
                text = stringResource(R.string.update_password),
                onClick = {
                    viewModel.changePassword(
                        currentPass = currentPassword,
                        newPass = newPassword,
                        confirmPass = confirmPassword
                    )
                },
                enabled = !uiState.isLoading,
                isLoading = uiState.isLoading
            )
        }
    }
}