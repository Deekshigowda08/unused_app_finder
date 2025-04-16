package com.example.guhyatainterntask.viewmodel

import android.app.*
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.UserHandle
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.*
import com.example.guhyatainterntask.model.AppInfo
import java.util.*

class AppViewModel(application: Application) : AndroidViewModel(application) {
    //To hold selected apps
    val selectedApp = mutableStateOf<AppInfo?>(null)

    //A helper function to update the selectedApp
    fun setSelectedApp(app: AppInfo) {
        selectedApp.value = app
    }

    private val _apps = mutableStateListOf<AppInfo>()
    val apps: List<AppInfo> get() = _apps

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
        resolvedApps.forEach { info ->
            val appInfo = info.activityInfo
            val appName = pm.getApplicationLabel(appInfo.applicationInfo).toString()
            val drawable = pm.getApplicationIcon(appInfo.applicationInfo)
            val icon = drawable.toBitmap().asImageBitmap()
            val version = try {
                pm.getPackageInfo(appInfo.packageName, 0).versionName ?: "N/A"
            } catch (e: Exception) {
                "Unknown"
            }

            //gets last-usage
            val lastUsed = usageStatsMap[appInfo.packageName]?.lastTimeUsed ?: 0L

            //gets app size
            val appSize = getAppSize(context, appInfo.packageName)

            // adding to apps data to _apps list
            _apps.add(
                AppInfo(
                    name = appName,
                    packageName = appInfo.packageName,
                    icon = icon,
                    version = version,
                    lastUsedTime = lastUsed,
                    sizeInBytes = appSize
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
            android.os.Process.myUid(),
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

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
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

    // Function to get app size (API 26+)
    private fun getAppSize(context: Context, packageName: String): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                val storageStats = storageStatsManager.queryStatsForPackage(
                    StorageManager.UUID_DEFAULT,
                    packageName,
                    UserHandle.getUserHandleForUid(appInfo.uid)
                )
                storageStats.appBytes + storageStats.dataBytes + storageStats.cacheBytes
            } catch (e: Exception) {
                0L
            }
        } else {
            0L
        }
    }
}

