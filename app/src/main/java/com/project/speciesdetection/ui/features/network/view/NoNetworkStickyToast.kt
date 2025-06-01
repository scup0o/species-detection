package com.project.speciesdetection.ui.features.network.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speciesdetection.R
import com.project.speciesdetection.domain.provider.network.ConnectivityObserver
import com.project.speciesdetection.ui.features.network.viewmodel.NetworkViewModel

@Composable
fun NoNetworkStickyToast(
    networkViewModel: NetworkViewModel = hiltViewModel()
) {
    val networkStatus by networkViewModel.networkStatus.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = networkStatus != ConnectivityObserver.Status.Available
    ) {
        Box(
            modifier = Modifier
                .padding(10.dp)
                .clip(shape = MaterialTheme.shapes.large)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(5.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.no_network_connection), color = MaterialTheme.colorScheme.onTertiary
                )

            }
        }

    }

}