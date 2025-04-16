package com.example.guhyatainterntask.model

import androidx.compose.ui.graphics.ImageBitmap

//data class to store the app releated information
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: ImageBitmap, // Use Bitmap instead of Drawable
    val version: String,
    val lastUsedTime: Long
)