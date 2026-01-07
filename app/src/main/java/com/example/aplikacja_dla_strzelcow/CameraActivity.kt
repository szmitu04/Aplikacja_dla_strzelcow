package com.example.aplikacja_dla_strzelcow


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import com.example.aplikacja_dla_strzelcow.cv.TargetDetector
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import com.example.aplikacja_dla_strzelcow.cv.TargetDetectionResult
import com.example.aplikacja_dla_strzelcow.cv.drawTargetOverlay

class CameraActivity : ComponentActivity() {

    private lateinit var imageCapture: ImageCapture

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // nic – kamera uruchomi się gdy PreviewView się pojawi
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var analyzedBitmap by remember { mutableStateOf<Bitmap?>(null) }
            var detectionResult by remember { mutableStateOf<TargetDetectionResult?>(null) }
            var photoFile by remember { mutableStateOf<File?>(null) }
            var isAnalyzing by remember { mutableStateOf(false) }
            CameraScreen(
                photoFile = photoFile,
                analyzedBitmap = analyzedBitmap,
                isAnalyzing = isAnalyzing,
                onTakePhoto = {
                    takePhoto { file ->
                        photoFile = file
                        //analyzedBitmap = null
                        //detectionResult = null
                    }
                },
                onRetry = {

                    photoFile = null
                    //analyzedBitmap = null
                    //detectionResult = null
                },
                onAccept = {
                    isAnalyzing = true

                    val originalBitmap = loadBitmapWithRotation(photoFile!!)
                    val result = TargetDetector.detect(originalBitmap)

                    if (result == null) {
                        isAnalyzing = false
                        return@CameraScreen
                    }

                    // 🔴 RYSUJEMY OVERLAY
                    val overlayBitmap = drawTargetOverlay(
                        originalBitmap,
                        result.centerX,
                        result.centerY,
                        result.radius
                    )

                    val overlayFile = File(
                        externalCacheDir,
                        "photo_overlay_${System.currentTimeMillis()}.jpg"
                    )

                    saveBitmapToFile(overlayBitmap, overlayFile)

                    // 🔴 ZWRACAMY ŚCIEŻKĘ DO AddSeriesActivity
                    val intent = Intent().apply {
                        // Zwracamy ścieżkę do pliku z nałożoną otoczką DO WYŚWIETLENIA
                        putExtra("overlayPhotoPath", overlayFile.absolutePath)
                        // Zwracamy ścieżkę do oryginalnego pliku DO WYSŁANIA
                        putExtra("originalPhotoPath", photoFile!!.absolutePath)
                        putExtra("cx", result.centerX)
                        putExtra("cy", result.centerY)
                        putExtra("radius", result.radius)
                    }
//                    val intent = Intent().apply {
//                        // ZMIANA: Zwracamy ścieżkę do pliku z nałożoną otoczką
//                        putExtra("photoPath", overlayFile.absolutePath)
//                        putExtra("cx", result.centerX)
//                        putExtra("cy", result.centerY)
//                        putExtra("radius", result.radius)
//                    }

                    setResult(Activity.RESULT_OK, intent)
                    finish()
//                    val intent = Intent().apply {
//                        putExtra("photoPath", photoFile!!.absolutePath)
//                        putExtra("cx", result.centerX)
//                        putExtra("cy", result.centerY)
//                        putExtra("radius", result.radius)
//                    }
//
//                    setResult(Activity.RESULT_OK, intent)
//                    finish()
                },
                onPreviewReady = { previewView ->
                    startCamera(previewView)
                }
            )
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera(previewView: PreviewView) {
        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({
            val cameraProvider = providerFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(onSaved: (File) -> Unit) {
        val file = File(externalCacheDir, "photo_${System.currentTimeMillis()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSaved(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                }
            }
        )
    }
}

@Composable
fun CameraScreen(
    photoFile: File?,
    analyzedBitmap: Bitmap?,
    isAnalyzing: Boolean,
    onTakePhoto: () -> Unit,
    onRetry: () -> Unit,
    onAccept: () -> Unit,
    onPreviewReady: (PreviewView) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        if (photoFile == null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PreviewView(context).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        onPreviewReady(this)
                    }
                }
            )

            Button(
                onClick = onTakePhoto,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            ) {
                Text("Zrób zdjęcie")
            }

        } else {
            val bitmap = remember(photoFile) {
                loadBitmapWithRotation(photoFile)
            }
//            val bitmap = remember(photoFile) {
//                loadBitmapWithRotation(photoFile)
//            }
            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Analizowanie…", color = Color.White)
                }
            }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = onRetry) {
                    Text("Ponów")
                }
                Button(onClick = onAccept) {
                    Text("OK")
                }
            }
        }
    }
}

fun loadBitmapWithRotation(file: File): Bitmap {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    val exif = ExifInterface(file.absolutePath)

    val rotation = when (
        exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    if (rotation == 0f) return bitmap

    val matrix = Matrix().apply { postRotate(rotation) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun saveBitmapToFile(bitmap: Bitmap, file: File) {
    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }
}
//class CameraActivity : ComponentActivity() {
//
//    private lateinit var previewView: PreviewView
//    private lateinit var imageCapture: ImageCapture
//
//    private val permissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
//            if (granted) startCamera()
//        }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//
//        setContent {
//            Aplikacja_dla_strzelcowTheme {
//                Box(modifier = Modifier.fillMaxSize()) {
//
//                    CameraPreview { pv ->
//                        previewView = pv
//                    }
//
//                    Button(
//                        onClick = {
//                            takePhoto { file ->
//                                val result = Intent().apply {
//                                    putExtra("photoPath", file.absolutePath)
//                                }
//                                setResult(RESULT_OK, result)
//                                finish()
//                            }
//                        },
//                        modifier = Modifier
//                            .align(Alignment.BottomCenter)
//                            .padding(24.dp)
//                    ) {
//                        Text("Zrób zdjęcie")
//                    }
//                }
//            }
//        }
//
//        if (ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.CAMERA
//            ) == PackageManager.PERMISSION_GRANTED
//        ) {
//            startCamera()
//        } else {
//            permissionLauncher.launch(Manifest.permission.CAMERA)
//        }
//    }
//
//    private fun startCamera() {
//        val providerFuture = ProcessCameraProvider.getInstance(this)
//
//        providerFuture.addListener({
//            val cameraProvider = providerFuture.get()
//
//            val preview = Preview.Builder().build().also {
//                it.setSurfaceProvider(previewView.surfaceProvider)
//            }
//
//            imageCapture = ImageCapture.Builder().build()
//
//            cameraProvider.unbindAll()
//            cameraProvider.bindToLifecycle(
//                this,
//                CameraSelector.DEFAULT_BACK_CAMERA,
//                preview,
//                imageCapture
//            )
//
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    private fun takePhoto(onSaved: (File) -> Unit) {
//        val file = File(
//            externalCacheDir,
//            "target_${System.currentTimeMillis()}.jpg"
//        )
//
//        val options = ImageCapture.OutputFileOptions.Builder(file).build()
//
//        imageCapture.takePicture(
//            options,
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                    onSaved(file)
//                }
//
//                override fun onError(exception: ImageCaptureException) {
//                    exception.printStackTrace()
//                }
//            }
//        )
//    }
//}
//
//@Composable
//fun CameraPreview(onReady: (PreviewView) -> Unit) {
//    val context = LocalContext.current
//
//    AndroidView(
//        modifier = Modifier.fillMaxSize(),
//        factory = {
//            PreviewView(context).apply {
//                scaleType = PreviewView.ScaleType.FILL_CENTER
//                onReady(this)
//            }
//        }
//    )
//}
//    private fun startCamera() {
//        val providerFuture = ProcessCameraProvider.getInstance(this)
//
//        providerFuture.addListener({
//            val cameraProvider = providerFuture.get()
//
//            val preview = Preview.Builder().build().also {
//                it.setSurfaceProvider(previewView.surfaceProvider)
//            }
//
//            imageCapture = ImageCapture.Builder()
//                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//                .build()
//
//            cameraProvider.unbindAll()
//            cameraProvider.bindToLifecycle(
//                this,
//                CameraSelector.DEFAULT_BACK_CAMERA,
//                preview,
//                imageCapture
//            )
//        }, ContextCompat.getMainExecutor(this))
//    }