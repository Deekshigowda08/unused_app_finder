package com.example.guhyatainterntask.ui.theme
import android.content.Context
import kotlinx.coroutines.delay
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guhyatainterntask.helper.hasUsageStatsPermission
import com.example.guhyatainterntask.helper.isAppUnused
import com.example.guhyatainterntask.model.AppInfo
import com.example.guhyatainterntask.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@JvmOverloads
@Composable
fun InstalledAppsScreen(
    viewModel: AppViewModel = viewModel(),
    onAppClick: (AppInfo) -> Unit
) {
    val apps = viewModel.apps
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("Unused apps") }
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var appToUninstall by remember { mutableStateOf<AppInfo?>(null) }
    var showList by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }


    // Ask for permission once
    LaunchedEffect(Unit) {
        if (!hasUsageStatsPermission(context)) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
        }
    }

    // Slight delay to allow Compose to settle before drawing large lists
    LaunchedEffect(apps) {
        showList = false
        showLoading = true
        delay(200)
        showLoading = false
        showList = true
    }

    // Filtered apps list with remember to prevent re-computation
    val filteredApps by produceState(initialValue = emptyList<AppInfo>(), apps, selectedTab, searchQuery) {
        delay(100) // debounce delay
        value = apps.filter { app ->
            val matchesTab = when (selectedTab) {
                "Unused apps" -> isAppUnused(app.lastUsedTime)
                else -> true
            }
            val matchesSearch = app.name.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header and search
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(Color.Black)
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Installed Apps",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Search")
                            }
                        }
                    }
                )
            }
        }

        // Tab Switcher
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF0F0F0))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Unused apps", "All Apps").forEach { tab ->
                    val selected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) Color.White else Color(0xFFF0F0F0))
                            .clickable {
                                context.vibrate()
                                selectedTab = tab
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // App List
        if (showLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                CircularProgressIndicator()
            }
        }
        else if (showList) {
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "No apps match your search.",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    itemsIndexed(
                        filteredApps,
                        key = { index, app -> "${app.packageName}-$index" }
                    ) { _, app ->
                        val formattedTime = formatLastUsedTime(app.lastUsedTime)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 9.dp)
                                .clickable { onAppClick(app) }
                                .animateItemPlacement()
                        ) {
                            AppIcon(app.icon)
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(text = app.name, fontWeight = FontWeight.Bold)
                                Text(text = formattedTime, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.weight(.5f))
                            Text(
                                text = "|",
                                modifier = Modifier.padding(vertical = 15.dp),
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.weight(.3f))

                            if (selectedTab == "Unused apps") {
                                IconButton(onClick = {
                                    appToUninstall = app
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Uninstall")
                                }
                            } else {
                                IconButton(onClick = {
                                    val launchIntent =
                                        context.packageManager.getLaunchIntentForPackage(app.packageName)
                                    launchIntent?.let { context.startActivity(it) }
                                }) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = "Open App")
                                }
                            }
                        }
                    }

                    // Total Count Display
                    item {
                        val totalLabel = if (selectedTab == "Unused apps") {
                            "Unused apps found: ${filteredApps.size}"
                        } else {
                            "Total installed apps: ${filteredApps.size}"
                        }

                        Text(
                            text = totalLabel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Bottom Spacer
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }

            }
        }

        // Uninstall Dialog
        if (showDialog && appToUninstall != null) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    appToUninstall = null
                },
                title = { Text("Uninstall Suggestion") },
                text = { Text("You have not used this app for a long while so you can uninstall ${appToUninstall?.name}?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.openAppInfo(context, appToUninstall!!.packageName)
                        showDialog = false
                        appToUninstall = null
                    }) {
                        Text("Go to AppInfo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false
                        appToUninstall = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


@Composable
fun AppIcon(bitmap: ImageBitmap) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier.size(48.dp)
    )
}


// customize date pattern
@Composable
fun formatLastUsedTime(timestamp: Long): String {
    return if (timestamp == 0L) {
        "Not used for a long time"
    } else {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        "Last used: ${formatter.format(date)}"
    }
}

// Helps to make vibrate
fun Context.vibrate(milliseconds: Long = 50) {
    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(milliseconds)
    }
}