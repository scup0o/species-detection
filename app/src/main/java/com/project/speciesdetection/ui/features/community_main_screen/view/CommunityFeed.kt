package com.project.speciesdetection.ui.features.community_main_screen.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.ui.features.community_main_screen.viewmodel.CommunityFeedViewModel


@Composable
fun CommunityFeed(
    navController: NavHostController,
    viewModel: CommunityFeedViewModel = hiltViewModel()
){
    val open by viewModel.searchQuery.collectAsState()

    Scaffold(
        bottomBar = {BottomNavigationBar(navController)}
    ){
        innerPadding ->
        Column {Text(
            modifier = Modifier.padding(innerPadding),
            text="community")
            Button(
                modifier = Modifier.padding(innerPadding),
                onClick={viewModel.updateSearchQuery(true)},
            ){
                Text("open")
            }
            if (open){
                Text("a")
            }  }

    }
}