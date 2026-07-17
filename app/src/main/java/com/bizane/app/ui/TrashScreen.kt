package com.bizane.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizane.app.data.AppSettings
import com.bizane.app.data.FoodSyncService
import com.bizane.app.data.TrashEntry
import com.bizane.app.ui.theme.CardBG
import com.bizane.app.ui.theme.PageBG
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** وەکو TrashViewController ـی iOS — لیستی ئایتمە سڕاوەکانی گروپ، تەنیا بۆ ئەدمین */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(onClose: () -> Unit) {
    var entries by remember { mutableStateOf<List<TrashEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var reloadTick by remember { mutableStateOf(0) }

    fun reload() {
        loading = true
        FoodSyncService.fetchTrash(AppSettings.groupId) { result ->
            entries = result
            loading = false
        }
    }

    LaunchedEffect(reloadTick) { reload() }

    val dateFmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    Scaffold(
        containerColor = PageBG,
        topBar = {
            TopAppBar(
                title = { Text("تەنەکەی خۆڵ", color = Color.White) },
                navigationIcon = { TextButton(onClick = onClose) { Text("داخستن", color = Color.White) } },
                actions = {
                    if (entries.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirm = true }) {
                            Text("بەتاڵکردنەوە", color = Color(0xFFFF3B30))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBG)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
                entries.isEmpty() -> Text(
                    "هیچ ئایتمێک نەسڕاوەتەوە",
                    color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.firestoreId ?: it.hashCode().toString() }) { e ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardBG)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(e.name, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                                Text(
                                    "سڕاوەتەوە لەلایەن ${e.deletedByName}" +
                                        (e.ownerName?.let { " (خاوەن: $it)" } ?: "") +
                                        " • ${dateFmt.format(Date(e.deletedAt))}",
                                    color = Color.Gray, fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("دڵنیایی؟") },
            text = { Text("هەموو تۆمارەکانی تەنەکەی خۆڵ بەتاڵ دەکرێنەوە.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    FoodSyncService.clearTrash(AppSettings.groupId) { reloadTick++ }
                }) { Text("بەڵێ، بەتاڵی بکەوە", color = Color(0xFFFF3B30)) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("نەخێر") } }
        )
    }
}
