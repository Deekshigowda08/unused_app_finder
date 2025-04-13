package com.example.guhyatainterntask.viewmodel

import android.app.*
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.*
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.guhyatainterntask.model.AppInfo
import java.util.*

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val selectedApp = mutableStateOf<AppInfo?>(null)


    fun setSelectedApp(app: AppInfo) {
        selectedApp.value = app
    }



    private val _apps = mutableStateListOf<AppInfo>()
    val apps: List<AppInfo> get() = _apps

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        val context = getApplication<Application>().applicationContext
        val pm = context.packageManager

        val usageStatsMap = getUsageStatsMap(context)

        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val resolvedApps = pm.queryIntentActivities(intent, 0)
        _apps.clear()

        resolvedApps.forEach { info ->
            val appInfo = info.activityInfo
            val appName = pm.getApplicationLabel(appInfo.applicationInfo).toString()
            val icon = pm.getApplicationIcon(appInfo.applicationInfo)
            val version = try {
                pm.getPackageInfo(appInfo.packageName, 0).versionName ?: "N/A"
            } catch (e: Exception) {
                "Unknown"
            }

            val lastUsed = usageStatsMap[appInfo.packageName]?.lastTimeUsed ?: 0L

            _apps.add(
                AppInfo(
                    name = appName,
                    packageName = appInfo.packageName,
                    icon = icon,
                    version = version,
                    lastUsedTime = lastUsed
                )
            )
        }
    }

    private fun getUsageStatsMap(context: Context): Map<String, UsageStats> {
        // Check if permission is granted
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )

        if (mode != AppOpsManager.MODE_ALLOWED) {
            // Optional: open settings to request permission
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

            // Return empty or notify user
            return emptyMap()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -30) // check usage for the last 30 days
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, // more flexible than INTERVAL_DAILY
            startTime,
            endTime
        )

        return stats.associateBy { it.packageName }
    }


    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }


}
