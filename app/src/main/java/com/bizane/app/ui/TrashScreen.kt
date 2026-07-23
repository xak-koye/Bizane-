package com.bizane.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizane.app.data.LocalTrashStorage
import com.bizane.app.data.TrashItem
import com.bizane.app.ui.theme.CardBG
import com.bizane.app.ui.theme.PageBG
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** پەڕەی تایبەت بۆ ئایتمە سڕاوەکان — وێنە، ناو، کاتی سڕینەوە، و دوگمەی گەڕاندنەوە بۆ هەر ئایتمێک. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(onClose: () -> Unit, onRestore: () -> Unit) {
    var entries by remember { mutableStateOf(LocalTrashStorage.entries.toList()) }
    var showClearConfirm by remember { mutableStateOf(false) }

    fun reload() { entries = LocalTrashStorage.entries.toList() }

    val dateFmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    Scaffold(
        containerColor = PageBG,
        topBar = {
            TopAppBar(
                title = { Text("سڕاوەکان", color = Color.White) },
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
            if (entries.isEmpty()) {
                Text(
                    "🧹\n\nهیچ ئایتمێکی سڕاوە نییە",
                    color = Color.Gray, fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(entries) { index, e ->
                        TrashRow(
                            entry = e,
                            dateFmt = dateFmt,
                            onRestore = {
                                val idx = LocalTrashStorage.entries.indexOfFirst { it === e }
                                if (idx >= 0) {
                                    val item = LocalTrashStorage.restore(idx)
                                    if (item != null) {
                                        com.bizane.app.data.FoodStorage.add(item)
                                        reload()
                                        onRestore()
                                    }
                                }
                            },
                            onDeleteForever = {
                                val idx = LocalTrashStorage.entries.indexOfFirst { it === e }
                                if (idx >= 0) { LocalTrashStorage.removeForever(idx); reload() }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("دڵنیایی؟") },
            text = { Text("هەموو تۆمارەکانی سڕاوەکان بەتاڵ دەکرێتەوە. ئەم کارە ناگەڕێتەوە.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    LocalTrashStorage.clear()
                    reload()
                }) { Text("بەڵێ، بەتاڵی بکەوە", color = Color(0xFFFF3B30)) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("نەخێر") } }
        )
    }
}

@Composable
private fun TrashRow(
    entry: TrashItem,
    dateFmt: SimpleDateFormat,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    val bmp = rememberItemBitmap(entry.item.imageBase64)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBG)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF333333)),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(entry.item.category.emoji, fontSize = 24.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.item.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text("سڕایەوە لە ${dateFmt.format(Date(entry.deletedAt))}", color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onDeleteForever) {
            Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = Color(0xFFFF3B30))
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A84FF))
                .clickable(onClick = onRestore)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("↩︎ گەڕاندنەوە", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
