package com.bizane.app.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.bizane.app.data.L
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * پەڕەی سکانکردنی بارکۆد — بەکارهێنانی Google Code Scanner (play-services-code-scanner) لە
 * جیاتی CameraX + ML Kit ـی خۆمان بۆنراو، چونکە ئەمە ڕاستەوخۆ لەلایەن Google بەڕێوەدەبرێت
 * (کامێرا + ئاشکراکردنی بارکۆد پێکەوە، متمانەپێکراوترە و کارا دەکات بەبێ پێویست بە
 * ڕێکخستنی خۆمانی CameraX). داواکاری ڕێگەپێدانی کامێرا خۆکار لەلایەن Google Play services
 * دەکرێت، پێویست بە Manifest.permission.CAMERA ی دەستی نییە.
 */
@Composable
fun BarcodeScannerScreen(onScanned: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val onScannedState = rememberUpdatedState(onScanned)
    val onCloseState = rememberUpdatedState(onClose)

    LaunchedEffect(Unit) {
        if (activity == null) {
            onCloseState.value()
            return@LaunchedEffect
        }
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E, Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93, Barcode.FORMAT_PDF417, Barcode.FORMAT_QR_CODE
            )
            .build()
        val scanner = GmsBarcodeScanning.getClient(activity, options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val value = barcode.rawValue
                if (!value.isNullOrEmpty()) onScannedState.value(value) else onCloseState.value()
            }
            .addOnCanceledListener {
                // بەکارهێنەر بەبێ سکانکردن گەڕایەوە — کێشە نییە
                onCloseState.value()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    L("add.barcodeNotFound") + (e.message?.let { ": $it" } ?: ""),
                    Toast.LENGTH_LONG
                ).show()
                onCloseState.value()
            }
    }

    // شاشەیەکی سادەی چاوەڕوانی — Google Code Scanner خۆی UI ی خۆی نیشان دەدات لەسەر ئەمە
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
        Button(
            onClick = { onCloseState.value() },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f))
        ) {
            Text(L("common.close"), color = Color.White)
        }
    }
}
