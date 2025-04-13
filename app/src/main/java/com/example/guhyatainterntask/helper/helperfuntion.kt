package com.example.guhyatainterntask.helper

import android.app.AppOpsManager
import android.content.Context

fun isAppUnused(lastUsedTime: Long): Boolean {
    if (lastUsedTime == 0L) return true
    val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
    return lastUsedTime < sevenDaysAgo
}
// A function to get access
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
