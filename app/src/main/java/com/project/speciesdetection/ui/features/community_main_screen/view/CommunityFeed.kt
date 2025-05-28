package com.project.speciesdetection.ui.features.community_main_screen.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.navigation.BottomNavigationBar
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

    if (authState.currentUser==null || !authState.currentUser!!.isEmailVerified){
        Scaffold(
            bottomBar = {BottomNavigationBar(navController)}
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)){
                Button(
                    onClick = {
                        navController.popBackStack(AppScreen.LoginScreen.route, inclusive = true, saveState = false)
                        navController.navigate(AppScreen.LoginScreen.route) {
                            launchSingleTop = true
                        }
                    }
                ) { Text("Login")}
                Button(
                    onClick = {
                        navController.popBackStack(AppScreen.SignUpScreen.route, inclusive = true, saveState = false)
                        navController.navigate(AppScreen.SignUpScreen.route) {
                            launchSingleTop = true
                        }
                    }
                ) { Text("SignUp")}
            }
        }
    }
    else{
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