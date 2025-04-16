package com.example.guhyatainterntask.helper

fun isAppUnused(lastUsedTime: Long): Boolean {
    if (lastUsedTime == 0L) return true
    val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
    return lastUsedTime < sevenDaysAgo
}