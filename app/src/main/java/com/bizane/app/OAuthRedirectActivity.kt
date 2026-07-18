package com.bizane.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.bizane.app.data.GoogleSignInHelper

/**
 * ئەم Activity ـە تەنیا کارەکەی وەرگرتنی redirect ـی OAuth ـە دوای چوونەژوورەوە بە Google لە Custom Tab ـەکە.
 * هیچ UI ـی نییە — دەستبەجێ ئەنجامەکە دەگەیەنێت بە GoogleSignInHelper و خۆی دادەخات.
 */
class OAuthRedirectActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        finish()
    }

    private fun handleIntent(intent: Intent?) {
        GoogleSignInHelper.handleRedirect(intent?.data)
    }
}
