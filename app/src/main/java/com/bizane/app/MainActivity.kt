package com.bizane.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.bizane.app.data.AppLang
import com.bizane.app.data.ExpiryNotifications
import com.bizane.app.data.FoodStorage
import com.bizane.app.ui.BizaneNavHost
import com.bizane.app.ui.theme.BizaneTheme
import com.bizane.app.ui.theme.PageBG

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FoodStorage.addSamplesIfNeeded()

        val requestNotifPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* هیچ کارێک ناکەین ئەگەر ڕەتی کردەوە — ئاگادارکردنەوە بەسادەیی کارا نابێت */ }

        setContent {
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !ExpiryNotifications.hasPermission(this@MainActivity)
                ) {
                    requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                // هاوشێوەی SceneDelegate.swift: کاتێک ئەپ دەکرێتەوە، پشکنین بکە ئایا
                // کاتی باکئەپی خۆکارانە هاتووە (بەپێی هەڵبژاردەی یوزەر لە ڕێکخستنەکاندا)
                com.bizane.app.data.AutoBackupManager.runIfNeeded()
            }
            // ئاراستەی ڕووکار بەپێی زمانی هەڵبژێردراو دەگۆڕدرێت (کوردی = RTL، English = LTR)
            val direction = if (AppLang.current.isRTL) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                BizaneTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = PageBG) {
                        BizaneNavHost()
                    }
                }
            }
        }
    }
}
