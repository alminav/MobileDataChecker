package com.almica.mobiledatachecker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaActionSound
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun GeoCameraScreen(
    onDismiss: (msg: Pair<String, String?>?, link: String?) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    var activeImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val resources = LocalResources.current
    val lifeCycle = LocalLifecycleOwner.current.lifecycle
    var isSaving by remember { mutableStateOf(false) }
    val actionSound = MediaActionSound()
    BackHandler {
        onDismiss(null, null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live-Kamera im Hintergrund anzeigen
        if (!isSaving) {
            CameraPreview(
                onReady = { captureObject ->
                    activeImageCapture = captureObject
                }
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.Center).fillMaxSize(0.3f).clickable(onClick = {
                isSaving = true
                takePicture(
                    actionSound,
                    context,
                    lifeCycle,
                    viewModel,
                    onDismiss = { msg, link ->
                        isSaving = false
                        onDismiss(msg, link)
                    },
                    activeImageCapture
                )
            })
        ) {
            if (isSaving) {
                // Zeigt den Ladekreis an, wenn das Bild verarbeitet wird
                CircularProgressIndicator(
                    color = Color.Red,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp).background(Color.White, CircleShape)
                )
            } else {
                // Standard Roter Punkt zum Auslösen
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Red, CircleShape)
                )
            }
        }
        // Close Button
        IconButton(
            onClick = { onDismiss(null, null) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Schließen"
            )
        }
        // 2. Button zum Auslösen über dem Live-Bild platzieren
        Button(
            onClick = {
                isSaving = true
                takePicture(
                    actionSound,
                    context,
                    lifeCycle,
                    viewModel,
                    onDismiss = { msg, link ->
                        isSaving = false
                        onDismiss(msg, link)
                    },
                    activeImageCapture
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(resources.getString(R.string.take_picture_and_upload))
        }
    }
}

private fun takePicture(
    actionSound: MediaActionSound,
    context: Context,
    lifeCycle: Lifecycle,
    viewModel: MainViewModel,
    onDismiss: (msg: Pair<String, String?>?, link: String?) -> Unit,
    activeImageCapture: ImageCapture?,
) {
    // 1. Akustisches Feedback (Kamera-Shutter-Sound)
    val deviceName = getDeviceName()
    actionSound.play(MediaActionSound.SHUTTER_CLICK)
    val imageCapture = activeImageCapture
    if (imageCapture != null) {
        val photoDate = getReadableDate(System.currentTimeMillis())
        val photoFile = File(context.cacheDir, "${deviceName}_${photoDate}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Lokales Foto existiert -> GPS holen und hochladen
                    // Aufruf innerhalb eines Coroutine-Scopes (z.B. lifecycleScope oder viewModelScope)

                    lifeCycle.coroutineScope.launch {
                        val compressedFile =
                            //compressImageWithLibrary(context, photoFile, photoFile)
                            compressImageFile(photoFile, photoFile)
                        Timber.i("Compressed file size: ${compressedFile.length()}")
                        fetchLocationAndSubmit(
                            file = compressedFile, //photoFile,
                            viewModel = viewModel,
                            onSuccess = { msg, link ->
                                actionSound.release()
                                onDismiss(Pair(photoFile.name, msg), link)
                            }
                        )
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onDismiss(Pair(photoFile.name, null), null)
                    actionSound.release()
                    Toast.makeText(context, "Fehler: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    } else {
        Toast.makeText(context, "Kamera wird noch geladen...", Toast.LENGTH_SHORT).show()
    }
}

@SuppressLint("MissingPermission")
private fun fetchLocationAndSubmit(
    file: File,
    viewModel: MainViewModel,
    onSuccess: (msg: String, link: String?) -> Unit
) {
        viewModel.uploadImageToBplaced(file) { response ->
            if (response.isSuccessful && response.body() != null) {
                val msg = response.body()!!.message
                val link = response.body()!!.url
                onSuccess(msg, link)
            } else {
                val msg = "Fehler beim Upload: ${response.code()}"
                onSuccess(msg, null)
            }
        }
}

/**
 * Komprimiert eine Bilddatei nativ auf unter 500 KB und speichert sie in einer neuen Datei.
 * Beachtet EXIF-Rotation, vermeidet OutOfMemoryErrors und optimiert Memory- & Disk-Zugriffe.
 */
suspend fun compressImageFile(
    inputFile: File,
    outputFile: File,
    targetSizeBytes: Long = 500 * 1024 // 500 KB
): File = withContext(Dispatchers.IO) {
    Timber.i("Compressing file: ${inputFile.name}")
    // 1. Schnellpfad: Wenn die Datei bereits klein genug ist
    if (inputFile.exists() && inputFile.length() <= targetSizeBytes) {
        if (inputFile != outputFile) {
            inputFile.copyTo(outputFile, overwrite = true)
        }
        return@withContext outputFile
    }

    // 2. EXIF-Rotation auslesen
    val rotationDegrees = getExifRotation(inputFile)

    // 3. Bildgrößen vorab prüfen & inSampleSize zur Speicherschonung berechnen
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(inputFile.absolutePath, options)

    // Maximal ~2048px für Kamerafotos beim ersten Laden
    options.inSampleSize = calculateInSampleSize(options, maxDimension = 2048)
    options.inJustDecodeBounds = false

    var bitmap = BitmapFactory.decodeFile(inputFile.absolutePath, options)
        ?: return@withContext inputFile

    // 4. EXIF-Rotation anwenden falls nötig
    if (rotationDegrees != 0) {
        val rotated = rotateBitmap(bitmap, rotationDegrees)
        if (rotated != bitmap) {
            bitmap.recycle()
            bitmap = rotated
        }
    }
    val newWidth = (bitmap.width * 0.5).toInt()
    val newHeight = (bitmap.height * 0.5).toInt()
    val scaledBitmap = bitmap.scale(newWidth, newHeight)
    if (scaledBitmap != bitmap) {
        bitmap.recycle()
        bitmap = scaledBitmap
    }

    // 5. In-Memory-Komprimierung (vermeidet wiederholte Festplatten-Schreibvorgänge)
    val stream = ByteArrayOutputStream()
    var quality = 100
    val minQuality = 80
    val minDimension = 200

    try {
        do {
            stream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            Timber.i("Compressed size: ${stream.size()} bytes (quality=$quality, dim=${bitmap.width}x${bitmap.height})")

            if (stream.size() <= targetSizeBytes) break

            if (quality > minQuality) {
                quality = (quality - 10).coerceAtLeast(minQuality)
            } else if (bitmap.width > minDimension && bitmap.height > minDimension) {
                val newWidth = (bitmap.width * 0.75).toInt()
                val newHeight = (bitmap.height * 0.75).toInt()

                if (newWidth < minDimension || newHeight < minDimension) break

                val scaledBitmap = bitmap.scale(newWidth, newHeight)
                if (scaledBitmap != bitmap) {
                    bitmap.recycle()
                    bitmap = scaledBitmap
                }
                quality = 85
            } else {
                break
            }
        } while (stream.size() > targetSizeBytes)

        // 6. Endergebnis einmalig auf Festplatte schreiben
        FileOutputStream(outputFile).use { out ->
            stream.writeTo(out)
        }
    } finally {
        bitmap.recycle() // Native Grafikressourcen freigeben
    }

    return@withContext outputFile
}

private fun getExifRotation(file: File): Int {
    return try {
        val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
        when (exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED)) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (_: Exception) {
        0
    }
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
    val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun calculateInSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > maxDimension || width > maxDimension) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onReady: (ImageCapture) -> Unit // Gibt das fertige ImageCapture-Objekt an den Screen zurück
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize(),
        update = { _ ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                try {
                    cameraProvider.unbindAll()
                    // Wichtig: Sowohl preview ALS AUCH imageCapture an den Lifecycle binden
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                    // Signalisiert dem Screen, dass bereit zum Fotografieren ist
                    onReady(imageCapture)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}
fun getReadableDate(millis: Long): String {
    // 1. Das gewünschte Datumsformat definieren (Locale.GERMANY für deutsche Monatsnamen/Formate)
    val timeFormatter =  SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", java.util.Locale.getDefault())

    // 2. Aus den Millisekunden ein Date-Objekt erstellen
    val netDate = Date(millis)

    // 3. Formatieren
    return timeFormatter.format(netDate)
}