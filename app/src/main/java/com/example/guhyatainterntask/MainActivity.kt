package com.example.guhyatainterntask

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.guhyatainterntask.ui.theme.AppDetailsScreen
import com.example.guhyatainterntask.ui.theme.GuhyataInternTaskTheme
import com.example.guhyatainterntask.ui.theme.InstalledAppsScreen
import com.example.guhyatainterntask.viewmodel.AppViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuhyataInternTaskTheme {
                val navController = rememberNavController()
                val viewModel: AppViewModel = viewModel()
                val context = LocalContext.current

                NavHost(navController, startDestination = "installed_apps") {
                    composable("installed_apps") {
                        InstalledAppsScreen(viewModel, onAppClick = { app ->
                            viewModel.setSelectedApp(app) // Save app in ViewModel
                            navController.navigate("app_details")
                        })
                    }
                    composable("app_details") {
                        val selectedApp = viewModel.selectedApp
                        selectedApp.value?.let {
                            AppDetailsScreen(
                                appName = it.name,
                                appVersion = it.version ?: "Unknown",
                                appIcon = it.icon,
                                onBackClick = { navController.popBackStack() },
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



