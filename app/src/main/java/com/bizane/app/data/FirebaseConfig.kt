package com.bizane.app.data

// زانیارییەکانی پڕۆژەی Firebase — هەمان پڕۆژەی iOS، بۆیە هەردوو ئەپەکە هەمان گروپ/داتا هاوبەش دەکەن
object FirebaseConfig {
    const val apiKey = "AIzaSyCf2KMhEpOvuetIbxstxWn_DVoqpVR2G6M"
    const val projectId = "besarchoo-6ec1d"
    const val firestoreBase =
        "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"

    // MARK: - Sign in with Google (لە Google Cloud Console دروستی بکە، جۆری "Android")
    // ڕێنمایی: https://console.cloud.google.com/apis/credentials -> Create Credentials -> OAuth client ID -> Android
    // Package name: com.bizane.app
    // SHA-1: لە keystore ـی release/debug ـەکەت وەریبگرە (بەم فەرمانە: ./gradlew signingReport)
    // تێبینی: ئەم client ID ـە جیاوازە لەوەی iOS — دەبێت client ID ـی نوێی جۆری Android دروست بکەیت.
    const val googleClientID = "YOUR_ANDROID_OAUTH_CLIENT_ID.apps.googleusercontent.com"

    // شێوازی redirect: هەمان نمونەی iOS (reversed client ID)، بۆ نموونە:
    // ئەگەر client ID بریتی بێت لە "1008704975133-xxxx.apps.googleusercontent.com"،
    // ئەم شێوازە دەبێت: "com.googleusercontent.apps.1008704975133-xxxx"
    // — پێویستە لەگەڵ intent-filter ـی OAuthRedirectActivity لە AndroidManifest.xml بگونجێت.
    const val googleRedirectScheme = "com.googleusercontent.apps.YOUR_ANDROID_OAUTH_CLIENT_ID"
}
