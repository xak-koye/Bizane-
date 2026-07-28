package com.bizane.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bizane.app.data.L
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * پەڕەی سکانکردنی بارکۆد — CameraX (پیشاندانی کامێرا) + ML Kit (ئاشکراکردنی بارکۆد لەسەر
 * خودی مۆبایل).
 *
 * جێگۆڕکێی GmsBarcodeScanning (play-services-code-scanner) کراوە بەمە، چونکە ئەوە پشتی
 * بە "Dynamic Feature Delivery" ی Play Store دەبەست بۆ داگرتنی مۆدیوولی سکانکردن — کارا
 * نییە لەسەر ئەپێک کە وەک APK ی سەربەخۆ دابەش دەکرێت (بڕوانە build-apk.yml)، بۆیە
 * startScan() هەرگیز success یان failure نەدەگەڕایەوە و شاشەکە هەتاهەتایە لە لۆدینگ
 * دەمایەوە. ئەم شێوازە بە تەواوی لەناو خودی ئەپەکەدا کاردەکات و هیچ پابەندیەکی
 * Play Store ی نییە.
 */
@Composable
fun BarcodeScannerScreen(onScanned: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onScannedState = rememberUpdatedState(onScanned)
    val onCloseState = rememberUpdatedState(onClose)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, L("add.cameraPermissionMsg"), Toast.LENGTH_LONG).show()
            onCloseState.value()
        }
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (hasCameraPermission) {
            CameraPreviewWithScanner(
                onFound = { code -> onScannedState.value(code) },
                onError = {
                    Toast.makeText(context, L("add.barcodeNotFound"), Toast.LENGTH_LONG).show()
                    onCloseState.value()
                }
            )
        } else {
            CircularProgressIndicator(color = Color.White)
        }
        Button(
            onClick = { onCloseState.value() },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f))
        ) {
            Text(L("common.close"), color = Color.White)
        }
    }
}

@Composable
private fun CameraPreviewWithScanner(onFound: (String) -> Unit, onError: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val alreadyReported = remember { AtomicBoolean(false) }

    val scannerOptions = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E, Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93, Barcode.FORMAT_PDF417, Barcode.FORMAT_QR_CODE
            )
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(scannerOptions) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(input)
                        .addOnSuccessListener { barcodes ->
                            val value = barcodes.firstOrNull { !it.rawValue.isNullOrEmpty() }?.rawValue
                            if (value != null && alreadyReported.compareAndSet(false, true)) {
                                onFound(value)
                            }
                        }
                        .addOnFailureListener {
                            // شکستی هەندێک فرەیم گرنگ نییە، دووبارە هەوڵ دەدرێتەوە بۆ فرەیمی داهاتوو
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                    )
                } catch (e: Exception) {
                    onError()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
