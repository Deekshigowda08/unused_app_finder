package com.example.guhyatainterntask.viewmodel

import android.Manifest
import android.app.AppOpsManager
import android.app.Application
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import com.example.guhyatainterntask.model.AppInfo
import java.util.Calendar

class AppViewModel(application: Application) : AndroidViewModel(application) {
    //To hold selected apps
    val selectedApp = mutableStateOf<AppInfo?>(null)

    //A helper function to update the selectedApp
    fun setSelectedApp(app: AppInfo) {
        selectedApp.value = app
    }

    private val _apps = mutableStateListOf<AppInfo>()
    val apps: List<AppInfo> get() = _apps


    fun reloadUsageDataOnly() {
        val context = getApplication<Application>().applicationContext
        val usageStatsMap = getUsageStatsMap(context)

        _apps.forEachIndexed { index, app ->
            // Retrieve existing permission status to avoid overwriting it if not re-checking
            val existingApp = _apps[index]
            val updated = app.copy(
                lastUsedTime = usageStatsMap[app.packageName]?.lastTimeUsed ?: 0L,
                // Preserve existing permission info if you are only reloading usage
                hasLocationPermission = existingApp.hasLocationPermission,
                hasPhonePermission = existingApp.hasPhonePermission
            )
            _apps[index] = updated
        }
    }

    fun reloadPermissions() { // Renamed for clarity
        val context = getApplication<Application>().applicationContext

        _apps.forEachIndexed { index, app ->
            // It's generally safer to work with a copy if you're modifying items in a list
            // you are iterating over, though direct modification by index is also possible
            // with MutableList.

            val updatedApp = app.copy(
                hasLocationPermission = checkAppPermission(context, app.packageName, Manifest.permission.ACCESS_FINE_LOCATION) ||
                        checkAppPermission(context, app.packageName, Manifest.permission.ACCESS_COARSE_LOCATION),
                hasPhonePermission = checkAppPermission(context, app.packageName, Manifest.permission.READ_PHONE_STATE) // Or other phone permissions
            )
            _apps[index] = updatedApp
        }
        // If _apps is a LiveData or StateFlow, you might need to update its value here
        // e.g., _appsLiveData.value = _apps.toList()
    }

    private fun checkAppPermission(context: Context, packageName: String, permission: String): Boolean {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val requestedPermissions = packageInfo.requestedPermissions
            if (requestedPermissions != null) {
                for (i in requestedPermissions.indices) {
                    if (requestedPermissions[i] == permission) {
                        // Check if the permission is granted for this app
                        // Note: This checks if the app *can* be granted the permission.
                        // To check if it *is currently* granted, you'd typically do this
                        // from within the app itself using ContextCompat.checkSelfPermission.
                        // Checking other apps' granted permissions is restricted for privacy.
                        // However, you can check if a permission is declared in its manifest.
                        return (packageManager.checkPermission(permission, packageName) == PackageManager.PERMISSION_GRANTED)
                    }
                }
            }
            false
        } catch (e: PackageManager.NameNotFoundException) {
            // App not found
            false
        }
    }


    //This block will be running as soon as this class is called
    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        val context = getApplication<Application>().applicationContext
        val pm = context.packageManager

        //gets app usage
        val usageStatsMap = getUsageStatsMap(context)

        //It’s a filter to get only the apps shown in the launcher.
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        //get apps which matchs intent
        val resolvedApps = pm.queryIntentActivities(intent, 0)
        _apps.clear()

        //gets Apps name icon and version
        resolvedApps.forEach { resolvedInfo -> // Renamed 'info' to 'resolvedInfo' for clarity
            val appActivityInfo = resolvedInfo.activityInfo // activityInfo from ResolveInfo
            val packageName = appActivityInfo.packageName

            val appName = pm.getApplicationLabel(appActivityInfo.applicationInfo).toString()
            val drawable = pm.getApplicationIcon(appActivityInfo.applicationInfo)
            val icon = drawable.toBitmap().asImageBitmap()
            val version = try {
                pm.getPackageInfo(packageName, 0).versionName ?: "N/A"
            } catch (e: Exception) {
                "Unknown"
            }

            //gets last-usage
            val lastUsed = usageStatsMap[packageName]?.lastTimeUsed ?: 0L

            // Check for permissions
            var hasLocation = false
            var hasPhone = false
            try {
                // Get PackageInfo for the application, not just the activity
                val packageInfo: PackageInfo =
                    pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                val requestedPermissions = packageInfo.requestedPermissions
                if (requestedPermissions != null) {
                    for (permission in requestedPermissions) {
                        if (permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                            permission == Manifest.permission.ACCESS_COARSE_LOCATION
                        ) {
                            hasLocation = true
                        }
                        if (permission == Manifest.permission.READ_PHONE_STATE ||
                            permission == Manifest.permission.CALL_PHONE // Add other phone permissions if needed
                        ) {
                            hasPhone = true
                        }
                        // If both found, no need to check further for this app
                        if (hasLocation && hasPhone) break
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // Handle the case where the package is no longer found (should be rare here)
                e.printStackTrace()
            }


            // adding to apps data to _apps list
            _apps.add(
                AppInfo(
                    name = appName,
                    packageName = packageName,
                    icon = icon,
                    version = version,
                    lastUsedTime = lastUsed,
                    hasLocationPermission = hasLocation, // Store permission info
                    hasPhonePermission = hasPhone      // Store permission info
                )
            )
        }
    }

    // Function to fetches usage stats for all apps used in the last 30 days.
    private fun getUsageStatsMap(context: Context): Map<String, UsageStats> {
        //to check if permission is granted
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )

        if (mode != AppOpsManager.MODE_ALLOWED) {
            // if not granted that to open settings to request permission
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

            // Return empty or notify user , where by using this emptymap function the code will not crash
            return emptyMap()
        }

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        // check usage for the last 30 days
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            // more flexible than INTERVAL_DAILY
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            endTime
        )
        // gives the mapped output
        return stats.associateBy { it.packageName }
    }

    // function to open the system App Info screen for the selected package where user can do what they want
    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

}