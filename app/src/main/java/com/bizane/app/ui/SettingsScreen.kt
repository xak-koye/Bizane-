package com.bizane.app.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizane.app.data.AppSettings
import com.bizane.app.data.FoodStorage
import com.bizane.app.ui.theme.CardBG
import com.bizane.app.ui.theme.PageBG

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: FoodViewModel,
    onOpenGroup: () -> Unit
) {
    val context = LocalContext.current
    val notifOptions = listOf(1, 3, 7)
    var notifSelected by remember { mutableStateOf(notifOptions.indexOf(AppSettings.notifDays).let { if (it < 0) 1 else it }) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PageBG,
        topBar = {
            TopAppBar(
                title = { Text("رێکخستنەکان", color = Color.White) },
                actions = {
                    TextButton(onClick = {
                        AppSettings.notifDays = notifOptions[notifSelected]
                        vm.refreshAfterEdit()
                    }) { Text("پاشەکەوت", color = Color.White, fontWeight = FontWeight.Bold) }
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
            SectionHeader("🔔  ئاگادارکردنەوە")
            Card {
                Text("ئاگادارکردنەوە پێش چەند رۆژ؟", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("١ رۆژ", "٣ رۆژ", "٧ رۆژ").forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = notifSelected == i,
                            onClick = { notifSelected = i },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = 3),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = androidx.compose.ui.graphics.Color(0xFF0A84FF),
                                activeContentColor = Color.White,
                                inactiveContainerColor = CardBG,
                                inactiveContentColor = Color.White
                            )
                        ) { Text(label) }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("↕️  ڕیزکردنی ئایتمەکان")
            Card {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SortMode.values().forEachIndexed { i, mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.setSortMode(mode) }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(mode.title, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            if (vm.sortMode.value == mode) {
                                Text("✓", color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        if (i != SortMode.values().lastIndex) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF383838)))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("👨‍👩‍👧‍👦  گروپی هاوبەش")
            val groupTitle = if (AppSettings.groupId.isEmpty()) "بەشداریکردن/دروستکردنی گروپ"
                else "گروپ: ${AppSettings.groupName}  (کۆد: ${AppSettings.groupCode})"
            ActionButton(groupTitle, color = if (AppSettings.groupId.isEmpty()) Color(0xFF0A84FF) else Color.White) { onOpenGroup() }

            Spacer(Modifier.height(20.dp))
            SectionHeader("📊  ئامار")
            StatsCard()

            Spacer(Modifier.height(20.dp))
            SectionHeader("🗑  داتا")
            ActionButton("سڕینەوەی هەموو خواردنەکان", color = Color(0xFFFF3B30)) { showClearConfirm = true }

            Spacer(Modifier.height(20.dp))
            SectionHeader("ℹ️  دەربارە")
            Card {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🍽️", fontSize = 34.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("بەسەرچوو!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("وەشان ١.٠", color = Color.Gray, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(1.dp).background(Color(0xFF383838)))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "دروستکراوە لەلایەن ستافی ئارا تیمەوە", color = Color.Gray,
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ئامانجی ئەپەکە ئاگاداربوونتە لە بەرواری بەسەرچوونی خواردنەکانت،\nتاوەکو هیچ خواردنێکت بەفیڕۆ نەچێت.",
                        color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("گەشەپێدەر: خاک کۆیی", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("📞  پەیوەندی")
            Card {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/khoshawe1/"))
                        context.startActivity(intent)
                    }
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF1877F2)),
                        contentAlignment = Alignment.Center
                    ) { Text("f", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("فەیسبووک", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("پەیوەندیمان پێوە بکە", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("دڵنیایی؟") },
            text = { Text("هەموو خواردنەکان دەسڕێتەوە. ئەم کارە ناگەڕێتەوە!") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    FoodStorage.items.toList().forEach { FoodStorage.delete(it.id) }
                    vm.refreshAfterEdit()
                }) { Text("بەڵێ، هەموویان بسڕەوە", color = Color(0xFFFF3B30)) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("نەخێر") } }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBG)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun ActionButton(title: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBG)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun StatsCard() {
    val items = FoodStorage.items
    val expired = items.count { it.isExpired }
    val soon = items.count { !it.isExpired && it.daysLeft <= 3 }
    val ok = items.count { it.daysLeft > 3 }

    Card {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem("${items.size}", "کۆی گشتی", Color.White)
            StatItem("$ok", "باش", Color(0xFF33D976))
            StatItem("$soon", "نزیک", Color(0xFFFF9500))
            StatItem("$expired", "بەسەرچوو", Color(0xFFFF3B30))
        }
    }
}

@Composable
private fun StatItem(num: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(num, color = color, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}
