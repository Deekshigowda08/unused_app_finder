package com.example.guhyatainterntask

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import android.provider.Settings
import android.widget.Toast
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
                    //added simple transition for smooth transition
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    //main screen
                    composable("installed_apps") {
                        InstalledAppsScreen(viewModel, onAppClick = { app ->
                            viewModel.setSelectedApp(app)
                            navController.navigate("app_details")
                        })
                    }
                    //details screen
                    composable("app_details") {
                        val selectedApp = viewModel.selectedApp.value

                        selectedApp?.let {
                            AppDetailsScreen(
                                appName = it.name,
                                appVersion = it.version ?: "Unknown",
                                appIcon = it.icon,
                                navController = navController,
                                onViewInPlayStore = {
                                    val packageName = it.packageName
                                    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))

                                    if (marketIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(marketIntent)
                                    } else if (browserIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(browserIntent)
                                    } else {
                                        Toast.makeText(context, "No app available to open Play Store link", Toast.LENGTH_SHORT).show()
                                    }

                                },
                                onOpenApp = {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(it.packageName)
                                    launchIntent?.let { intent ->
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }
                                },
                                onOpenAppInfo = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${it.packageName}")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
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




