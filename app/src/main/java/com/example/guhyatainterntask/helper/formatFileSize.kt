package com.example.guhyatainterntask.helper

//funtion to convert file size
fun formatFileSize(sizeInBytes: Long): String {
    val kb = 1024
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        sizeInBytes >= gb -> String.format("%.1fGB", sizeInBytes.toDouble() / gb)
        sizeInBytes >= mb -> String.format("%.1fMB", sizeInBytes.toDouble() / mb)
        sizeInBytes >= kb -> String.format("%.1fKB", sizeInBytes.toDouble() / kb)
        else -> "$sizeInBytes B"
    }
}