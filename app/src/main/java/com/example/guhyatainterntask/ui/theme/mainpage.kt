package com.example.guhyatainterntask.ui.theme
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guhyatainterntask.helper.isAppUnused
import com.example.guhyatainterntask.model.AppInfo
import com.example.guhyatainterntask.viewmodel.AppViewModel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab { Unused, All ,permission}

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnrememberedMutableState")
@JvmOverloads
@Composable
fun InstalledAppsScreen(
    viewModel: AppViewModel = viewModel(),
    onAppClick: (AppInfo) -> Unit
) {
    val apps = viewModel.apps
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Unused) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var appToUninstall by remember { mutableStateOf<AppInfo?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                viewModel.reloadPermissions()
                isRefreshing = false
            }
        }
    )
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (viewModel.apps.isNotEmpty()) {

                // Try reloading only if lastUsedTime is missing (e.g., all 0)
                val allZero = viewModel.apps.all { it.lastUsedTime == 0L }
                if (allZero) {
                    viewModel.reloadUsageDataOnly()
                }
            }
        }
    }


    // Properly debounced and cancelable filtering
    val filteredApps by produceState(initialValue = apps) {
        snapshotFlow { Triple(apps, selectedTab, searchQuery) }
            .debounce(100)
            .collect { (appsList, tab, query) ->
                if (appsList.isNotEmpty()) {
                    value = appsList.filter { app ->
                        val matchesTab = when (tab) {
                            AppTab.Unused -> isAppUnused(app.lastUsedTime)
                            AppTab.All -> true
                            AppTab.permission -> app.hasPhonePermission || app.hasLocationPermission
                        }
                        val matchesSearch = app.name.contains(query, ignoreCase = true)
                        matchesTab && matchesSearch
                    }
                }
            }
    }



        Column(modifier = Modifier.fillMaxSize()) {
            // Header and Search
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
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.LightGray
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search Icon")
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

            // Tabs
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
                    listOf(
                        AppTab.Unused to "Unused apps",
                        AppTab.All to "All Apps",
                        AppTab.permission to "Permissions"
                    ).forEach { (tabEnum, tabLabel) ->
                        val selected = tabEnum == selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) Color.White else Color(0xFFF0F0F0))
                                .clickable {
                                    context.vibrate()
                                    selectedTab = tabEnum
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabLabel,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredApps.isEmpty() && searchQuery.isNotEmpty()) {
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
            }
            else if (selectedTab==AppTab.permission) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    ) {
                        Text("Phone Permission Allowed Apps", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(20.dp))

                        Column {
                            apps.filter { it.hasPhonePermission }.forEach { app ->
                                AppRow(app, onAppClick)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Location Permission Allowed Apps", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(20.dp))

                        Column {
                            apps.filter { it.hasLocationPermission }.forEach { app ->
                                AppRow(app, onAppClick)
                            }
                        }
                    }

                    PullRefreshIndicator(
                        refreshing = isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }


            }
            else {
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
                                .clickable { onAppClick(app) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(app.icon)
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(text = app.name, fontWeight = FontWeight.Bold)
                                Text(text = formattedTime, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            if (selectedTab == AppTab.Unused) {
                                IconButton(onClick = {
                                    appToUninstall = app
                                    showDialog = true
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Uninstall ${app.name}"
                                    )
                                }
                            } else {
                                IconButton(onClick = {
                                    val launchIntent =
                                        context.packageManager.getLaunchIntentForPackage(app.packageName)
                                    launchIntent?.let { context.startActivity(it) }
                                }) {
                                    Icon(
                                        Icons.Default.ExitToApp,
                                        contentDescription = "Open ${app.name}"
                                    )
                                }
                            }
                        }
                    }

                    item {
                        val label = if (selectedTab == AppTab.Unused) {
                            "Unused apps found: ${filteredApps.size}"
                        } else {
                            "Total installed apps: ${filteredApps.size}"
                        }

                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }

            // Uninstall Suggestion Dialog
            if (showDialog && appToUninstall != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDialog = false
                        appToUninstall = null
                    },
                    title = { Text("Uninstall Suggestion") },
                    text = { Text("You have not used this app for a long while. Do you want to uninstall ${appToUninstall?.name}?") },
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




//Used to print app icons
@Composable
fun AppIcon(bitmap: ImageBitmap) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier.size(48.dp)
    )
}

@Composable
fun AppRow(app: AppInfo, onAppClick: (AppInfo) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onAppClick(app) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app.icon) // Assuming you have this composable
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = app.name, fontWeight = FontWeight.Bold)
    }
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