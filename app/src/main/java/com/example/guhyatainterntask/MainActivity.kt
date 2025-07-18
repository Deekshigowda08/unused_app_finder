package com.example.guhyatainterntask

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.guhyatainterntask.ui.theme.AppDetailsScreen
import com.example.guhyatainterntask.ui.theme.GuhyataInternTaskTheme
import com.example.guhyatainterntask.ui.theme.InstalledAppsScreen
import com.example.guhyatainterntask.viewmodel.AppViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {

    private fun executeShellCommandWithShizuku(command: String) {
        try {
            // Split the command into an array
            val cmd = command.split(" ").toTypedArray()

            // Execute the command using ShizukuRemoteProcess
            val process = Shizuku.newProcess(cmd, null, null)

            // Read the output from the process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            reader.close()

            // Print the output
            Log.d(TAG, output.toString())

            // Wait for the process to finish
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, e.message ?: "Unknown error")
        }
    }
    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        if (granted) {

            val packageName = "in.swiggy.android"
            val permission = "android.permission.CAMERA"
            val command = "pm revoke $packageName $permission"
            executeShellCommandWithShizuku(command)

        } else {
            Log.e(TAG, "Shizuku permission denied")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)

        // Check permission when needed
        checkShizukuPermission(requestCode = 123)
        enableEdgeToEdge()
        setContent {
            GuhyataInternTaskTheme {
                if (Shizuku.checkSelfPermission()
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    Shizuku.requestPermission(100) // request‑code
                }
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
    override fun onDestroy() {
        super.onDestroy()
        // Unregister the listener
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }
    private fun checkShizukuPermission(requestCode: Int): Boolean {
        return when {
            Shizuku.isPreV11() -> {
                false
            }
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> {
                true
            }
            Shizuku.shouldShowRequestPermissionRationale() -> {
                false
            }
            else -> {
                Shizuku.requestPermission(requestCode)
                false
            }
        }
    }
}




