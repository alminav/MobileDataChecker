package com.almica.mobiledatachecker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import timber.log.Timber

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val networkMonitor = NetworkMonitor(application)
    private val workManager = WorkManager.getInstance(application)
    private val prefs = PreferenceManager(application)

    val isMobileDataActive: StateFlow<Boolean> = networkMonitor.isMobileDataActive

    // Track work status more robustly by checking for any active work with the tag
    val isWorkerRunning: StateFlow<Boolean> = workManager
        .getWorkInfosByTagFlow(Constants.WORK_TAG)
        .map { list ->
            list.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _locationList = MutableStateFlow<List<LocationItem>>(emptyList())
    val locationList: StateFlow<List<LocationItem>> = _locationList.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getNotificationsEnabled())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.setNotificationsEnabled(enabled)
        _notificationsEnabled.value = enabled
    }

    val workerLastRunTime: StateFlow<Long> = workManager
        .getWorkInfosByTagFlow(Constants.WORK_TAG)
        .map { list ->
            if (list.isNotEmpty()) System.currentTimeMillis() else 0L
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val sendLocationCount: StateFlow<Int> = workManager
        .getWorkInfosByTagFlow(Constants.WORK_TAG)
        .map {
            prefs.getSendLocationCount()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = prefs.getSendLocationCount()
        )

    fun startWorker() {
        val interval = prefs.getIntervalMinutes()
        val workRequest = PeriodicWorkRequestBuilder<MobileDataCheckWorker>(
            interval, TimeUnit.MINUTES
        )
            .addTag(Constants.WORK_TAG)
            .build()

        // Use unique periodic work to avoid multiple instances and ensure correct status tracking
        workManager.enqueueUniquePeriodicWork(
            Constants.WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        prefs.resetSendLocationCount()
    }

    fun stopWorker() {
        // Cancel by tag ensures all instances (unique or tagged) are stopped
        workManager.cancelAllWorkByTag(Constants.WORK_TAG)
    }

    fun updatePreferences(phone: String, interval: Long) {
        val oldInterval = prefs.getIntervalMinutes()
        prefs.setPhoneNumber(phone)
        prefs.setIntervalMinutes(interval)

        // If worker is running and interval changed, restart it
        if (isWorkerRunning.value && oldInterval != interval) {
            startWorker()
        }
    }

    fun getPhoneNumber() = prefs.getPhoneNumber()
    fun getIntervalMinutes() = prefs.getIntervalMinutes()

    fun testWorkerImmediately() {
        val testRequest = OneTimeWorkRequestBuilder<MobileDataCheckWorker>()
            .addTag(Constants.WORK_TAG)
            .addTag("test_worker")
            .build()
        workManager.enqueue(testRequest)
    }

    fun fetchLocationsFromBplaced() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = NetworkClient.bplacedApiService.fetchLocations()
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.status == "success" && apiResponse.data != null) {
                        // Updating this StateFlow will trigger the collector in MainActivity
                        _locationList.value = apiResponse.data
                        Timber.i("Anzahl geladener Orte: ${apiResponse.data.size}")
                        for (location in apiResponse.data) {
                            Timber.i("Ort: ${location.title} (${location.latitude}, ${location.longitude}, ${location.altitude}, ${location.temperature})")
                        }
                    } else {
                        Timber.i("Fehler: ${apiResponse.message}")
                    }
                } else {
                    Timber.e("Server-Fehler: ${response.code()}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Netzwerkfehler: ${e.localizedMessage}")
            }
        }
    }

    fun executeLocationCleanup(feedBack: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = NetworkClient.bplacedApiService.deleteAllLocations()
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.status == "success") {
                        // Updating this StateFlow will trigger the collector in MainActivity
                        _locationList.value = emptyList()
                        val msg = apiResponse.message
                        feedBack(msg)
                        Timber.i(msg)
                    } else {
                        val msg = "Fehler: ${apiResponse.message}"
                        feedBack(msg)
                        Timber.e(msg)
                    }
                } else {
                    val msg = "Server-Fehler: ${response.code()}"
                    feedBack(msg)
                    Timber.e(msg)
                }
            } catch (e: Exception) {
                val msg = "Netzwerkfehler: ${e.localizedMessage}"
                feedBack(msg)
                Timber.e(msg)
            }
        }
    }

    fun executeLocationDeleteByFilter(filter: String, feedBack: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = NetworkClient.bplacedApiService.deleteLocationByFilter(filter)
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.status == "success") {
                        // Updating this StateFlow will trigger the collector in MainActivity
                        _locationList.value = emptyList()
                        val msg = apiResponse.message
                        feedBack(msg)
                        Timber.i(msg)
                    } else {
                        val msg = "Fehler: ${apiResponse.message}"
                        feedBack(msg)
                        Timber.e(msg)
                    }
                } else {
                    val msg = "Server-Fehler: ${response.code()}"
                    feedBack(msg)
                    Timber.e(msg)
                }
            } catch (e: Exception) {
                val msg = "Netzwerkfehler: ${e.localizedMessage}"
                feedBack(msg)
                Timber.e(e, msg)
            }
        }
    }

    fun executeDeleteOldRecords(days: Int, feedBack: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Parameter 'days' wird hier übergeben
                val response = NetworkClient.bplacedApiService.deleteOldRecords(days)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!

                        if (apiResponse.status == "success") {
                            val msg = apiResponse.message
                            feedBack(msg)
                            Timber.i(msg)
                            //fetchLocationsFromBplaced()
                        } else {
                            val msg = "Old records delete error: ${apiResponse.message}"
                            feedBack(msg)
                            Timber.e(msg)
                        }
                    } else {
                        val msg = "Server-Fehler: ${response.code()}"
                        feedBack(msg)
                        Timber.e(msg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = "Netzwerkfehler: ${e.localizedMessage}"
                    feedBack(msg)
                    Timber.e( "Netzwerkfehler: ${e.localizedMessage}")
                }
            }
        }
    }


    // Wir setzen den Standardwert auf null, falls kein Limit übergeben wird
    fun executeFetchLocationsCount(limit: Int? = null, feedBack: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Hier wird das Limit an die Retrofit-Schnittstelle übergeben
                val response = NetworkClient.bplacedApiService.getLocationsCount(count = limit)
                Timber.i("Anzahl der Orte: ${response.body()?.data?.size}")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!

                        if (apiResponse.status == "success" && apiResponse.data != null) {
                            // Updating this StateFlow will trigger the collector in MainActivity
                            _locationList.value = apiResponse.data
                            Timber.i("Anzahl geladener Orte: ${apiResponse.data.size}")
                            for (location in apiResponse.data) {
                                Timber.i("Ort: ${location.title} (${location.latitude}, ${location.longitude}, ${location.altitude}, ${location.temperature})")
                            }
                        } else {
                            Timber.i("Fehler: ${apiResponse.message}")
                        }
                    } else {
                        val msg = "Server-Fehler: ${response.code()}"
                        feedBack(msg)
                        Timber.e(msg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = "Netzwerkfehler: ${e.localizedMessage}"
                    feedBack(msg)
                    Timber.e( "Netzwerkfehler: ${e.localizedMessage}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitor.unregister()
    }
}
