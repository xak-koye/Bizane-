package com.bizane.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizane.app.data.AppSettings
import com.bizane.app.data.AutoBackupMode
import com.bizane.app.data.DriveBackup
import com.bizane.app.data.DriveTokenStore
import com.bizane.app.data.GoogleAuth
import com.bizane.app.data.L
import com.bizane.app.ui.theme.CardBG
import com.bizane.app.ui.theme.PageBG
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * پەڕەی هەژمار و باکئەپ — هاوشێوەی AccountBackupViewController.swift.
 * تا کاتێک GoogleAuth.isConfigured false بێت، دوگمە سەرەکییەکان کاریان پێ ناکرێت
 * و پەیامێکی ڕوون پیشان دەدرێت کە پێویستی بە ڕێکخستنی Google Cloud/Firebase هەیە.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBackupScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat("MMM d, yyyy — HH:mm", Locale.getDefault()) }

    var isLinked by remember { mutableStateOf(DriveTokenStore.isLinked) }
    var busy by remember { mutableStateOf<String?>(null) } // "backup" | "restore" | null
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var autoBackupMode by remember { mutableStateOf(AppSettings.autoBackupMode) }
    var showAutoBackupPicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PageBG,
        topBar = {
            TopAppBar(
                title = { Text(L("settings.accountSection"), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBG)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (!GoogleAuth.isConfigured) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF3A2A00))
                        .padding(14.dp)
                ) {
                    Text(
                        "⚠️ Google Sign-In / Drive backup isn't configured yet on this build " +
                            "(missing Google Cloud OAuth client + google-services.json). " +
                            "This screen is a working skeleton — hook up credentials to enable it.",
                        color = Color(0xFFFFCC66), fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            if (!isLinked) {
                Text(L("settings.unlinkedWarning"), color = Color(0xFFFF9500), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                BigButton(L("settings.linkGoogle"), enabled = GoogleAuth.isConfigured, color = Color(0xFF0A84FF)) {
                    GoogleAuth.signIn(context as android.app.Activity) { success, error ->
                        if (success) { isLinked = true }
                        else Toast.makeText(context, error ?: L("common.failed"), Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Text(
                    L("settings.linkedStatus", DriveTokenStore.accountEmail ?: ""),
                    color = Color(0xFF33D976), fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                val last = DriveTokenStore.lastBackupMillis
                Text(
                    if (last != null) L("settings.lastBackup", fmt.format(Date(last))) else L("settings.noBackupYet"),
                    color = Color.Gray, fontSize = 12.sp
                )

                Spacer(Modifier.height(20.dp))
                BigButton(
                    L("settings.autoBackupBtn", autoBackupMode.title),
                    enabled = true, color = CardBG
                ) { showAutoBackupPicker = true }

                Spacer(Modifier.height(10.dp))
                BigButton(
                    if (busy == "backup") L("settings.backingUp") else L("settings.backupBtn"),
                    enabled = busy == null, color = Color(0xFF0A84FF)
                ) {
                    busy = "backup"
                    scope.launch {
                        val result = DriveBackup.backup()
                        busy = null
                        val ok = result is DriveBackup.Result.Success
                        Toast.makeText(
                            context,
                            if (ok) L("settings.backupSuccessMsg") else L("common.failed"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                Spacer(Modifier.height(10.dp))
                BigButton(
                    if (busy == "restore") L("settings.restoring") else L("settings.restoreBtn"),
                    enabled = busy == null, color = CardBG
                ) { showRestoreConfirm = true }

                Spacer(Modifier.height(24.dp))
                TextButton(onClick = { showSignOutConfirm = true }) {
                    Text(L("settings.unlinkGoogle"), color = Color(0xFFFF3B30))
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text(L("settings.restoreConfirmTitle")) },
            text = { Text(L("settings.restoreConfirmMsg")) },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    busy = "restore"
                    scope.launch {
                        val result = DriveBackup.restore()
                        busy = null
                        val ok = result is DriveBackup.Result.Success
                        Toast.makeText(
                            context,
                            if (ok) L("settings.restoreSuccessMsg") else L("common.failed"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }) { Text(L("settings.restoreConfirmYes"), color = Color(0xFFFF3B30)) }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false }) { Text(L("common.cancel")) } }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(L("settings.signOutTitle")) },
            text = { Text(L("settings.signOutMsg")) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutConfirm = false
                    GoogleAuth.signOut()
                    isLinked = false
                }) { Text(L("settings.signOutConfirm"), color = Color(0xFFFF3B30)) }
            },
            dismissButton = { TextButton(onClick = { showSignOutConfirm = false }) { Text(L("common.cancel")) } }
        )
    }

    if (showAutoBackupPicker) {
        AlertDialog(
            onDismissRequest = { showAutoBackupPicker = false },
            title = { Text(L("settings.autoBackupQuestion")) },
            text = {
                Column {
                    AutoBackupMode.entriesList.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppSettings.autoBackupMode = mode
                                    autoBackupMode = mode
                                    showAutoBackupPicker = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                (if (mode == autoBackupMode) "✓  " else "     ") + mode.title,
                                color = if (mode == autoBackupMode) Color(0xFF0A84FF) else Color.White,
                                fontWeight = if (mode == autoBackupMode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAutoBackupPicker = false }) { Text(L("common.cancel")) } }
        )
    }
}

@Composable
private fun BigButton(title: String, enabled: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) color else color.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}
