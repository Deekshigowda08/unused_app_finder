package com.example.guhyatainterntask.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val version: String,
    val lastUsedTime: Long
)