package com.bizane.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.bizane.app.data.FoodStorage
import com.bizane.app.data.GoogleSignInHelper
import com.bizane.app.ui.BizaneNavHost
import com.bizane.app.ui.theme.BizaneTheme
import com.bizane.app.ui.theme.PageBG

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FoodStorage.addSamplesIfNeeded()

        setContent {
            // ئەپەکە بە زمانی کوردی (RTL) نووسراوە
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BizaneTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = PageBG) {
                        BizaneNavHost()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ئەگەر بەکارهێنەر لە Custom Tab ـی Google پاشگەز بووەوە بەبێ تەواوکردن، چاوەڕوانییەکە هەڵبوەشێنەوە
        GoogleSignInHelper.cancelPending()
    }
}
