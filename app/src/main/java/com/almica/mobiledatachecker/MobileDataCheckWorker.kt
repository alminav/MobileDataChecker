package com.almica.mobiledatachecker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class MobileDataCheckWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val (isEnabled, temp) = isMobileDataEnabled(applicationContext)
        if (isEnabled) {
            sendMobileDataNotification(applicationContext)
        }
        Timber.i("Mobile Data Check Worker executed, temperature: $temp°C")

        // 1. Check if the device is capable of sending SMS
        /*
        if (!applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)) {
            Timber.e("Device does not support SMS messaging.")
            return Result.failure()
        }
        */

        // 2. Check for permissions (SMS and Location)
        val hasSmsPermission = applicationContext.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val hasLocationPermission = applicationContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasSmsPermission) {
            try {
                //val smsManager = applicationContext.getSystemService(SmsManager::class.java)
                val prefs = PreferenceManager(applicationContext)
                val phoneNumber = prefs.getPhoneNumber()

                val location = if (hasLocationPermission) getCurrentLocation() else null
                val lat = location?.latitude ?: 0.0
                val lon = location?.longitude ?: 0.0
                val alt = location?.altitude ?: 0.0

                val deviceName = getDeviceName()

                //val message = "Temp: $temp°C | Loc: $lat,$lon"
                //smsManager.sendTextMessage(phoneNumber, null, message, null, null)

                Timber.i("INACTIVE: SMS sent to $phoneNumber for device $deviceName. Location: $lat, $lon")

                sendLocation(deviceName, lat, lon, altitude = alt, temperature = temp)
            } catch (e: Exception) {
                Timber.e(e, "Failed to send SMS")
                return Result.retry()
            }
        } else {
            Timber.w("SEND_SMS permission not granted. Cannot send text message.")
            return Result.failure()
        }

        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): android.location.Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        return try {
            // Request high accuracy location with a timeout/cancellation token
            val cts = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).await()

            location
        } catch (e: SecurityException) {
            Timber.e(e, "Location permission not granted")
            null
        } catch (e: Exception) {
            Timber.e(e, "Failed to get location")
            null
        }
    }
}
