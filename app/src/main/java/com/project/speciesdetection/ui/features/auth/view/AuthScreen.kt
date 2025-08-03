package com.project.speciesdetection.ui.features.auth.view

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
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
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import com.project.speciesdetection.ui.composable.common.ErrorScreenPlaceholder
import com.project.speciesdetection.ui.composable.common.ErrorText
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.auth.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AuthScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (authState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            if (authState.error.equals("network")) {
                Column(modifier = Modifier.align(Alignment.Center)) {
                    ErrorScreenPlaceholder {
                        authViewModel.checkCurrentUser()
                    }
                }
            } else {
                GlideImage(
                    model = R.drawable.auth_bg,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth().padding(top=50.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    /*Row(
                        verticalAlignment = Alignment.CenterVertically,){
                        GlideImage(
                            model = R.drawable.logo,
                            contentDescription = null,
                            modifier = Modifier
                                .height(50.dp)
                                .width(50.dp)
                        )
                        Text(
                            stringResource(R.string.app_name),
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
                    }*/

                }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter),
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(50.dp),
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                        ) {
                            Column {
                                val annotatedString = buildAnnotatedString {
                                    // Phần 1: "Chào mừng bạn đến với "
                                    append(stringResource(id = R.string.get_started_title))
                                    append("  ") // Thêm khoảng trắng

                                    // Đánh dấu vị trí sẽ chèn logo
                                    appendInlineContent("logo", "[logo]")

                                    // Phần 2: " IDmal"
                                    append(" ") // Thêm khoảng trắng
                                    append(stringResource(id = R.string.app_name)+"!")
                                }

                                // Map để định nghĩa nội dung cho "logo" đã đánh dấu ở trên
                                val inlineContent = mapOf(
                                    "logo" to InlineTextContent(
                                        // Placeholder định nghĩa kích thước cho logo trong dòng văn bản
                                        placeholder = Placeholder(
                                            width = 34.sp, // Kích thước logo, có thể điều chỉnh
                                            height = 34.sp,
                                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter // Căn logo theo giữa dòng chữ
                                        )
                                    ) {
                                        Box(
                                           Modifier.background(
                                                color = Color.White,
                                                shape = RoundedCornerShape(25)
                                            ).padding(4.dp)
                                        ){
                                            GlideImage(
                                                model = R.drawable.logo,
                                                contentDescription = "Logo", // Thêm mô tả cho accessibility
                                                modifier = Modifier.fillMaxSize()

                                            )
                                        }
                                        // Đây là Composable thực tế sẽ được hiển thị

                                    }
                                )

                                // Hiển thị Text đã được xây dựng
                                Text(
                                    text = annotatedString,
                                    inlineContent = inlineContent, // Áp dụng nội dung inline
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        color = Color.White,
                                        shadow = Shadow(
                                            color = Color.Black,
                                            offset = Offset(4f, 4f),
                                            blurRadius = 10f,
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
                                    ),
                                    modifier = Modifier.padding(top =10.dp)
                                )
                            }


                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {

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
                                            ambientShadowColor =
                                                Color.White // Màu của bóng (phát sáng)
                                            spotShadowColor = Color.White
                                        }
                                ) {
                                    Text(
                                        stringResource(R.string.get_started).uppercase(),
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            /*shadow = Shadow(
                                            color = Color.Black,
                                            offset = Offset(4f, 4f), // Độ lệch của bóng
                                            blurRadius = 51f,          // Độ mờ của bóng
                                        ),*/
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
                        Spacer(modifier = Modifier.height(100.dp))
                    }
            }

        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        ) {
            BottomNavigationBar(navController)
        }
    }
}