package com.bizane.app.data

/** زانیارییەکانی پڕۆژەی Firebase — دەبێت هەمان پڕۆژەی FirebaseConfig.swift بێت،
 *  چونکە داتابەیسی هاوبەش (SharedProductDB) دەبێت لەنێوان Android و iOS هاوبەش بێت. */
object FirebaseConfig {
    const val apiKey = "AIzaSyCf2KMhEpOvuetIbxstxWn_DVoqpVR2G6M"
    const val projectId = "besarchoo-6ec1d"
    const val firestoreBase =
        "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"
}
