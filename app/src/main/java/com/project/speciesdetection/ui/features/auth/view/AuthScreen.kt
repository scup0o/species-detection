package com.project.speciesdetection.ui.features.auth.view

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.ui.composable.common.ErrorText
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.auth.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AuthScreen(
    navController: NavHostController,
) {
    Scaffold(
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GlideImage(
                model = R.drawable.login_background,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ){
                Column(
                    verticalArrangement = Arrangement.spacedBy(100.dp),
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                ) {
                    Column {
                        Text(
                            stringResource(R.string.get_started_title),
                            style = MaterialTheme.typography.displaySmall.copy(
                                color = Color.White,
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(4f, 4f), // Độ lệch của bóng
                                    blurRadius = 10f,          // Độ mờ của bóng
                                ),
                                fontWeight = FontWeight.Bold,
                            )
                        )
                        Text(
                            stringResource(R.string.get_started_text),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(4f, 4f), // Độ lệch của bóng
                                    blurRadius = 10f,          // Độ mờ của bóng
                                ),
                            )
                        )
                    }


                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Button(
                            onClick = {
                                navController.popBackStack(
                                    AppScreen.LoginScreen.route,
                                    inclusive = true,
                                    saveState = false
                                )
                                navController.navigate(AppScreen.LoginScreen.route) {
                                    launchSingleTop = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .graphicsLayer {
                                    shadowElevation = 10.dp.toPx()
                                    shape = RoundedCornerShape(10.dp)
                                    clip = true
                                    ambientShadowColor = Color.White // Màu của bóng (phát sáng)
                                    spotShadowColor = Color.White
                                }
                        ) {
                            Text(
                                stringResource(R.string.get_started).uppercase(),
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    shadow = Shadow(
                                        color = Color.Black,
                                        offset = Offset(4f, 4f), // Độ lệch của bóng
                                        blurRadius = 51f,          // Độ mờ của bóng
                                    ),
                                )
                            )
                            Icon(
                                Icons.AutoMirrored.Default.ArrowForward, null
                            )
                        }

                        /*
                        Text(
                            stringResource(R.string.login_divider)
                        )

                        Button(
                            onClick = {
                                navController.popBackStack(
                                    AppScreen.SignUpScreen.route,
                                    inclusive = true,
                                    saveState = false
                                )
                                navController.navigate(AppScreen.SignUpScreen.route) {
                                    launchSingleTop = true
                                }
                            }
                        ) { Text("SignUp") }*/
                    }
                }


                Row(
                ) {
                    BottomNavigationBar(navController)
                }
            }






        }
    }
}