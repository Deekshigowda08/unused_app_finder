package com.example.guhyatainterntask

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.guhyatainterntask.ui.theme.AppDetailsScreen
import com.example.guhyatainterntask.ui.theme.GuhyataInternTaskTheme
import com.example.guhyatainterntask.ui.theme.InstalledAppsScreen
import com.example.guhyatainterntask.viewmodel.AppViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable

@OptIn(ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuhyataInternTaskTheme {
                val navController = rememberNavController()
                val viewModel: AppViewModel = viewModel()
                val context = LocalContext.current

                AnimatedNavHost(
                    navController = navController,
                    startDestination = "installed_apps",
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    composable("installed_apps") {
                        InstalledAppsScreen(viewModel, onAppClick = { app ->
                            viewModel.setSelectedApp(app)
                            navController.navigate("app_details")
                        })
                    }

                    composable("app_details") {
                        val selectedApp = viewModel.selectedApp.value
                        selectedApp?.let {
                            AppDetailsScreen(
                                appName = it.name,
                                appVersion = it.version ?: "Unknown",
                                appIcon = it.icon,
                                navController = navController,
                                onViewInPlayStore = {
                                    val uri = Uri.parse("market://details?id=${it.packageName}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}




