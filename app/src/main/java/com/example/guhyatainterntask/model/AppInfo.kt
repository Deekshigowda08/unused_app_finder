package com.example.guhyatainterntask.model

import androidx.compose.ui.graphics.ImageBitmap

//Data class to store the app releated information
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: ImageBitmap,
    val version: String,
    val lastUsedTime: Long,
    val sizeInBytes:Long
)