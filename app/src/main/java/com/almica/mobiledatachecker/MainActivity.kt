package com.almica.mobiledatachecker

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val viewModel: MainViewModel = viewModel()
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isMobileDataLive by viewModel.isMobileDataActive.collectAsStateWithLifecycle(initialValue = false)
    val isWorkerRunning by viewModel.isWorkerRunning.collectAsStateWithLifecycle(initialValue = false)
    val workerLastRunTime by viewModel.workerLastRunTime.collectAsStateWithLifecycle(initialValue = 0L)
    val sendLocationCount by viewModel.sendLocationCount.collectAsStateWithLifecycle(initialValue = 0)

    var lastStatusChangeTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val locationList by viewModel.locationList.collectAsStateWithLifecycle()
    var showLocationDialog by remember { mutableStateOf(false) }
    var showSmsDialog by remember { mutableStateOf(false) }

    var hasRequiredPermissions by remember { mutableStateOf(false) }
    var showBackgroundRationale by remember { mutableStateOf(false) }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Timber.i("Background location permission granted: $isGranted")
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasRequiredPermissions = results.values.all { it }

        val foregroundLocationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (foregroundLocationGranted) {
            val hasBackground = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasBackground) {
                showBackgroundRationale = true
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        // Standard permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.add(Manifest.permission.READ_PHONE_STATE)
        permissions.add(Manifest.permission.SEND_SMS)

        // Foreground location permissions
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.INTERNET)
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE)

        permissionLauncher.launch(permissions.toTypedArray())

        // Note: ACCESS_BACKGROUND_LOCATION should ideally be requested
        // only after foreground permissions are granted, and often requires
        // a separate UI explanation to the user as per Google Play policies.
    }

    // Update timestamp when status changes or worker runs

    LaunchedEffect(isMobileDataLive, isWorkerRunning, workerLastRunTime) {
        lastStatusChangeTime = System.currentTimeMillis()
    }

    // Auto-notification when live status becomes active
    LaunchedEffect(isMobileDataLive) {
        if (isMobileDataLive && hasRequiredPermissions) {
            sendMobileDataNotification(context)
        }
    }

    // When location list is updated, if it's not empty, show the dialog.
    // The dialog itself handles resetting showLocationDialog to false via onDismiss.
    // If you want to prevent it showing multiple times for the SAME list,
    // you would typically clear the list in the ViewModel after dismissing.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Standort Monitor") },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            MainScreenContent(
                isMobileDataLive = isMobileDataLive,
                isWorkerRunning = isWorkerRunning,
                workerInterval = viewModel.getIntervalMinutes(),
                sendLocationCount = sendLocationCount,
                lastStatusChangeTime = lastStatusChangeTime,
                onCheckStatus = {
                    if (isMobileDataLive && hasRequiredPermissions) {
                        sendMobileDataNotification(context)
                    }
                    lastStatusChangeTime = System.currentTimeMillis()
//                    val intent = Intent(context, BplacedActivity::class.java)
//                    context.startActivity(intent)
                },
                onStartWorker = {
                    viewModel.startWorker()
                },
                onStopWorker = { viewModel.stopWorker() },
                onSendLocationNow = {
                    viewModel.testWorkerImmediately()
                    lastStatusChangeTime = System.currentTimeMillis()
                },
                onSendSmsNow = {
                    showSmsDialog = true
                },
                onFetchLocations = {
                viewModel.fetchLocationsFromBplaced()
                showLocationDialog = true
            })
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentPhone = viewModel.getPhoneNumber(),
            currentInterval = viewModel.getIntervalMinutes(),
            onDismiss = { showSettingsDialog = false },
            onSave = { phone, interval ->
                viewModel.updatePreferences(phone, interval)
                showSettingsDialog = false
            }
        )
    }

    if (showLocationDialog) {
        LocationListDialog(
            deviceName = getDeviceName(),
            locations = locationList,
            onDismiss = {
                showLocationDialog = false
            },
            onCleanup = {filter ->
                Timber.i("Filter: $filter")
                if (filter.isBlank())
                    viewModel.executeLocationCleanup()
                else
                    viewModel.executeLocationDeleteByFilter(filter)
                showLocationDialog = false

            }
        )
    }

    if (showBackgroundRationale) {
        AlertDialog(
            onDismissRequest = { showBackgroundRationale = false },
            title = { Text("Hintergrund-Standortzugriff") },
            text = {
                Text("Diese App benötigt Zugriff auf Ihren Standort im Hintergrund.\n\nBitte wähle in den folgenden Einstellungen 'Immer zulassen'.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackgroundRationale = false
                        backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                ) {
                    Text("Zulassen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundRationale = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showSmsDialog) {
        SendSmsDialog(
            initialPhone = viewModel.getPhoneNumber(),
            onDismiss = { showSmsDialog = false },
            onSend = { phone, message ->
                showSmsDialog = false
                val hasSmsPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED

                if (hasSmsPermission) {
                    try {
                        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.getSystemService(SmsManager::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            SmsManager.getDefault()
                        }
                        smsManager.sendTextMessage(phone, null, message, null, null)
                        Toast.makeText(context, "SMS gesendet an $phone", Toast.LENGTH_SHORT).show()
                        Timber.i("SMS sent to $phone: $message")
                    } catch (e: Exception) {
                        Timber.e(e, "Fehler beim Senden der SMS")
                        Toast.makeText(context, "Fehler beim Senden der SMS: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "SEND_SMS Berechtigung nicht erteilt", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentPhone: String,
    currentInterval: Long,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit
) {
    var phone by remember { mutableStateOf(currentPhone) }
    var intervalStr by remember { mutableStateOf(currentInterval.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Einstellungen") },
        text = {
            Column(modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("SMS Telefonnummer") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = intervalStr,
                    onValueChange = { if (it.all { char -> char.isDigit() }) intervalStr = it },
                    label = { Text("Check Intervall (Minuten, min. 15)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val interval = intervalStr.toLongOrNull() ?: 15L
                    onSave(phone, if (interval < 15) 15L else interval)
                }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun SendSmsDialog(
    initialPhone: String,
    initialMessage: String = "Test SMS vom Standort Monitor",
    onDismiss: () -> Unit,
    onSend: (phone: String, message: String) -> Unit
) {
    var phone by remember { mutableStateOf(initialPhone) }
    var message by remember { mutableStateOf(initialMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SMS senden") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Empfänger Telefonnummer") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("SMS Nachricht") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (phone.isNotBlank() && message.isNotBlank()) {
                        onSend(phone, message)
                    }
                },
                enabled = phone.isNotBlank() && message.isNotBlank()
            ) {
                Text("Senden")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

private fun Double.format(digits: Int): String = String.format(Locale.getDefault(), "%.${digits}f", this)

@Composable
fun LocationListDialog(
    locations: List<LocationItem>,
    onDismiss: () -> Unit,
    onCleanup: (String) -> Unit,
    deviceName: String
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredLocations = remember(locations, searchQuery) {
        if (searchQuery.isBlank()) {
            locations
        } else {
            locations.filter {
                it.title?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Standorte") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Device Filter") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 8.dp),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        searchQuery = if (searchQuery.isBlank())
                            deviceName
                        else
                            ""
                    }) {
                        Icon(imageVector = Icons.Default.TabletAndroid, contentDescription = "Filter by device",
                            tint = if (searchQuery.isBlank()) MaterialTheme.colorScheme.primary else
                                MaterialTheme.colorScheme.error)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    filteredLocations.forEach { location ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val gmmIntentUri =
                                        "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}".toUri()
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    context.startActivity(mapIntent)
                                    searchQuery = location.title.orEmpty()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = location.title.takeUnless { it.isNullOrBlank() }
                                    ?: "Ort #${location.id}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Koord: ${location.latitude.format(5)}°, ${location.longitude.format(5)}°, ${location.altitude.format(0)}m",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (location.created_at.isNotBlank()) {
                                Text(
                                    text = "Datum: ${location.created_at}, Temp: ${location.temperature?.format(1)}°C",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(onClick = { onCleanup(searchQuery) }) {
                Text("Cleanup")
            }
        }
    )
}

@Composable
fun WorkerControlCard(
    isWorkerRunning: Boolean,
    workerInterval: Long,
    sendLocationCount: Int,
    onStartWorker: () -> Unit,
    onStopWorker: () -> Unit,
    onFetchLocations: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = CardDefaults.cardColors().containerColor,
    contentColor: Color = CardDefaults.cardColors().contentColor,
    onSendLocationNow: () -> Unit,
    onSendSmsNow: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Gesendete Standorte: $sendLocationCount",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            // Start Job Button
            OutlinedButton(
                onClick = onStartWorker,
                enabled = !isWorkerRunning,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Start, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(modifier = Modifier.weight(1f),
                        text = if (isWorkerRunning) {
                            stringResource(R.string.worker_running, workerInterval)
                        } else {
                            stringResource(R.string.worker_start, workerInterval)
                        }
                    )
                }
            }

// Stop Job Button
            OutlinedButton(
                onClick = onStopWorker,
                enabled = isWorkerRunning,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        tint = if (isWorkerRunning) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(modifier = Modifier.weight(1f),
                        text = stringResource(R.string.worker_stop))
                }
            }

            // Fetch Locations Button
            OutlinedButton(
                onClick = onFetchLocations,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(modifier = Modifier.weight(1f), text = stringResource(R.string.get_locations))
                }
            }
            // Send Location now
            OutlinedButton(
                onClick = onSendLocationNow,
                enabled = isWorkerRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(modifier = Modifier.weight(1f), text = "Standort jetzt senden")
                }
            }
            // Send SMS now
            OutlinedButton(
                onClick = onSendSmsNow,
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(modifier = Modifier.weight(1f), text = "SMS jetzt senden")
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    isMobileDataLive: Boolean,
    lastStatusChangeTime: Long,
    statusBoxColor: Color,
    contentColor: Color,
    timeFormatter: SimpleDateFormat,
    onCheckStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedTime = remember(lastStatusChangeTime) {
        timeFormatter.format(Date(lastStatusChangeTime))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusBoxColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Aktueller Status $formattedTime:",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (isMobileDataLive) "AKTIVIERT (Mobilnetz)" else "DEAKTIVIERT / WLAN",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onCheckStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("Status jetzt prüfen")
                }
            }
        }
    }
}

@Composable
fun MainScreenContent(
    isMobileDataLive: Boolean,
    isWorkerRunning: Boolean,
    workerInterval: Long,
    sendLocationCount: Int,
    lastStatusChangeTime: Long,
    onCheckStatus: () -> Unit,
    onStartWorker: () -> Unit,
    onStopWorker: () -> Unit,
    onSendLocationNow: () -> Unit,
    onFetchLocations: () -> Unit,
    onSendSmsNow: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val deviceName = getDeviceName()
    val statusBoxColor by animateColorAsState(
        targetValue = if (isMobileDataLive) MaterialTheme.colorScheme.errorContainer 
                      else MaterialTheme.colorScheme.primaryContainer,
        label = "StatusColor"
    )
    
    val contentColor = if (isMobileDataLive) MaterialTheme.colorScheme.onErrorContainer 
                       else MaterialTheme.colorScheme.onPrimaryContainer
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
/*        Text(
            text = "Locations Monitor",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )*/
        Text(
            text = "Device: $deviceName",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text(stringResource(R.string.actions)) }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text(stringResource(R.string.status)) }
            )
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> {
                    WorkerControlCard(
                        isWorkerRunning = isWorkerRunning,
                        workerInterval = workerInterval,
                        sendLocationCount = sendLocationCount,
                        onStartWorker = onStartWorker,
                        onStopWorker = onStopWorker,
                        onFetchLocations = onFetchLocations,
                        onSendLocationNow = onSendLocationNow,
                        onSendSmsNow = onSendSmsNow
                    )
                }
                1 -> {
                    StatusCard(
                        isMobileDataLive = isMobileDataLive,
                        lastStatusChangeTime = lastStatusChangeTime,
                        statusBoxColor = statusBoxColor,
                        contentColor = contentColor,
                        timeFormatter = timeFormatter,
                        onCheckStatus = onCheckStatus,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Active")
@Composable
fun MainScreenActivePreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreenContent(
                isMobileDataLive = true,
                isWorkerRunning = false,
                workerInterval = 15L,
                sendLocationCount = 3,
                lastStatusChangeTime = System.currentTimeMillis(),
                onCheckStatus = {},
                onStartWorker = {},
                onStopWorker = {},
                onSendLocationNow = {},
                onFetchLocations = {},
                onSendSmsNow = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Inactive")
@Composable
fun MainScreenInactivePreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreenContent(
                isMobileDataLive = false,
                isWorkerRunning = true,
                workerInterval = 15L,
                sendLocationCount = 5,
                lastStatusChangeTime = System.currentTimeMillis(),
                onCheckStatus = {},
                onStartWorker = {},
                onStopWorker = {},
                onSendLocationNow = {},
                onFetchLocations = {},
                onSendSmsNow = {}
            )
        }
    }
}

