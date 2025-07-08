package com.project.speciesdetection.ui.composable.common.species

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Divider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.speciesdetection.domain.model.species.ConservationStatusInfo
import com.project.speciesdetection.domain.model.species.getChipColors
import com.project.speciesdetection.domain.model.species.iucnStatuses
import com.project.speciesdetection.R


@Composable
fun IUCNConservationStatusView(
    currentStatusCode: String?
) {
    val currentStatus = remember(currentStatusCode) {
        iucnStatuses.find { it.code.equals(currentStatusCode, ignoreCase = true) }
    }
    val uriHandler = LocalUriHandler.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        // Hàng cho các nhãn nhóm
        if (currentStatusCode=="domestic"){
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.iucn_status_domesticate), // Sử dụng stringResource
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )}
        }
        else{
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.iucn_group_extinct), style=MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Box(modifier = Modifier.height(6.dp).width(1.dp).background(Color.DarkGray).offset(y = 2.dp))
                    }
                }
                Box(modifier = Modifier.weight(3f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.iucn_group_threatened), style=MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        HorizontalDivider(
                            modifier = Modifier.width(100.dp).padding(top = 1.dp),
                            thickness = 1.dp,
                            color = Color.DarkGray
                        )
                    }
                }
                Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.iucn_group_least_concern), style=MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Box(modifier = Modifier.height(6.dp).width(1.dp).background(Color.DarkGray).offset(y = 2.dp))
                    }
                }
            }

            // Hàng cho các chip trạng thái (giữ nguyên, không cần thay đổi vì nó dùng abbreviation)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp)
            ) {
                iucnStatuses.forEach { statusInfo ->
                    ConservationStatusChip(
                        statusInfo = statusInfo, // Truyền cả object statusInfo
                        isSelected = statusInfo.code.equals(currentStatusCode, ignoreCase = true)
                    )
                }
            }

            // Hiển thị tên đầy đủ và thông tin IUCN
            if (currentStatus != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(id = currentStatus.fullNameResId), // Sử dụng stringResource
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    /*Text(
                        text = stringResource(R.string.iucn_reference_text), // "(IUCN 3.1)"
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )*/

                    val iucnLinkUrl = stringResource(R.string.iucn_reference_url)
                    val annotatedLinkString = buildAnnotatedString {
                        pushStringAnnotation(tag = "IUCN_REF", annotation = iucnLinkUrl)
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.outline, textDecoration = TextDecoration.None)) {
                            append(stringResource(R.string.iucn_reference_text))
                        }
                        pop()
                    }
                    /*ClickableText(
                        text = annotatedLinkString,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                        onClick = { offset ->
                            annotatedLinkString.getStringAnnotations(tag = "IUCN_REF", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    try {
                                        uriHandler.openUri(annotation.item)
                                    } catch (e: Exception) {
                                        // Log.e("IUCNLink", "Could not open URI", e)
                                    }
                                }
                        },
                        modifier = Modifier.padding(start = 2.dp)
                    )*/
                }
            } else if (currentStatusCode != null && currentStatusCode.isNotBlank()){
                Text(
                    stringResource(R.string.iucn_status_unknown) + ": $currentStatusCode", // "Unknown status: VU"
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ConservationStatusChip(
    statusInfo: ConservationStatusInfo,
    isSelected: Boolean
) {
    val (bgColor, textColor) = getChipColors(statusInfo, isSelected)
    val borderColor = if (isSelected) bgColor else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .size(40.dp) // Kích thước của vòng tròn
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = statusInfo.abbreviation,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

