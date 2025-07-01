package com.project.speciesdetection.ui.features.setting_main_screen.view

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.speciesdetection.R
import com.project.speciesdetection.app.MainActivity
import com.project.speciesdetection.core.helpers.LocaleHelper.setLanguage
import com.project.speciesdetection.domain.model.LanguageItem
import com.project.speciesdetection.ui.features.setting_main_screen.viewmodel.SettingViewModel
import java.util.Locale

@Composable
fun LanguagePickerDialog(
    viewModel: SettingViewModel,
    onBackPressed:(Boolean) -> Unit,
){
    val currentLanguageCode by viewModel.currentLanguageCode.collectAsState()
    val supportedLanguages = viewModel.supportedLanguages
    val context = LocalContext.current
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = {viewModel.onCloseLanguagePicker()},
        confirmButton = {
            TextButton(
                onClick = {viewModel.onCloseLanguagePicker()},

                ){
                Text(stringResource(R.string.dismiss).uppercase(Locale.getDefault()))
            }
        },
        title = {
            Text(
                stringResource(R.string.language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                item {
                }
                items(supportedLanguages) { languageItem ->
                    LanguageRow(
                        languageItem = languageItem,
                        isSelected = languageItem.code == currentLanguageCode,
                        onLanguageSelected = {
                            if (languageItem.code != currentLanguageCode) {

                                onBackPressed(true)
                                viewModel.onLanguageSelected(languageItem.code)
                                setLanguage(context, languageItem.code)
                                (context as? Activity)?.recreate()
                                val intent = Intent(context, MainActivity::class.java) // Hoặc (context as Activity).javaClass
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                (context as? Activity)?.startActivity(intent)
                                (context as? Activity)?.finishAffinity()
                            }
                        }
                    )
                }

            }
        }
    )

}

@Composable
fun LanguageRow(
    languageItem: LanguageItem,
    isSelected: Boolean,
    onLanguageSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLanguageSelected)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = languageItem.displayName, style = MaterialTheme.typography.bodyLarge)
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}