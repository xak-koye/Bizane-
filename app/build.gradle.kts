plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bizane.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bizane.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.activity:activity-compose:1.9.0")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")

    // On-device text recognition (auto-fill name from photo)
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Barcode scanning: CameraX preview + on-device ML Kit barcode detection.
    // پێشتر GmsBarcodeScanning (play-services-code-scanner) بەکاردەهات، بەڵام ئەوە
    // پشت بە Play Store ی خۆی دادەبەست بۆ داگرتنی مۆدیوولەکەی سکانکردن (Dynamic Feature
    // Delivery)، کە تەنیا کاردەکات ئەگەر ئەپەکە لە Play Store ـەوە دامەزرابێت. چونکە
    // ئەم ئەپە وەک APK ـی سەربەخۆ دابەش دەکرێت (بڕوانە build-apk.yml)، ئەو مۆدیوولە
    // هەرگیز دانادەمەزرێت و startScan() هەتاهەتایە لە چاوەڕوانیدا دەمێنێتەوە (بێ هیچ
    // success/failure callback ـێک) — ئەمە هۆکاری ئەو کێشەیە بوو کە سکانکردن بێ بڕیار
    // بە لۆدینگ دەمایەوە. CameraX + ML Kit ـی خوارەوە بە تەواوی لەناو خودی ئەپەکەدا
    // کاردەکات و پشت بە Play Store نابەستێت.
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Coroutines (used by the Open Food Facts lookup + ML Kit task bridging)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.2.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
