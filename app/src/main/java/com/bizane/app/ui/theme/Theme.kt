package com.bizane.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// هەمان ڕەنگەکانی iOS
val PageBG = Color(0xFF12121A)   // UIColor(0.07, 0.07, 0.09)
val CardBG = Color(0xFF212129)   // UIColor(0.13, 0.13, 0.16)
val FieldBG = Color(0xFF292929)  // UIColor(white: 0.16)
val ChipBG = Color(0xFF333333)   // UIColor(white: 0.20)
val IconBtnBG = Color(0xFF383838) // UIColor(white: 0.22)
val StatusGreen = Color(0xFF33D976)
val StatusYellow = Color(0xFFFFCC00)
val StatusOrange = Color(0xFFFF9500)
val StatusRed = Color(0xFFFF3B30)
val GraySecondary = Color(0xFF8E8E93)

private val DarkColors = darkColorScheme(
    background = PageBG,
    surface = CardBG,
    primary = Color.White,
    onPrimary = PageBG,
    onBackground = Color.White,
    onSurface = Color.White,
    error = StatusRed
)

@Composable
fun BizaneTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
