package com.project.speciesdetection.ui.features.community_main_screen.view

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.firebase.messaging.FirebaseMessaging
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.ui.composable.common.NotificationPermissionEffect
import com.project.speciesdetection.ui.features.community_main_screen.viewmodel.CommunityFeedViewModel
import com.project.speciesdetection.ui.features.auth.view.AuthScreen
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel


@Composable
fun CommunityFeed(
    containerColor : Color? = MaterialTheme.colorScheme.background,
    navController: NavHostController,
    viewModel: CommunityFeedViewModel = hiltViewModel(),
    authViewModel: AuthViewModel
){
    val open by viewModel.searchQuery.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState.currentUser) {
        if (authState.currentUser!=null){
            authViewModel.reloadCurrentUser(authState.currentUser!!)
        }
    }

    if (authState.currentUserInformation==null){
        AuthScreen(navController)
    }
    else{

        val context = LocalContext.current

        // Lấy FirebaseMessaging instance
        val fcm = FirebaseMessaging.getInstance()

        // Sử dụng Composable đã tạo
        NotificationPermissionEffect(
            key = authState.currentUserInformation!!.uid, // Effect sẽ re-trigger khi user đăng nhập/đăng xuất
            onPermissionGranted = {
                authViewModel.updateFcmToken()

            },
            onPermissionDenied = {
            }
        )

        Log.i("check check", authState.currentUserInformation.toString())
        Scaffold(
            containerColor = containerColor!!,
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

}