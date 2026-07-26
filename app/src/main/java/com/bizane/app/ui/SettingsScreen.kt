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
import com.bizane.app.data.AppLang
import com.bizane.app.data.AppLanguage
import com.bizane.app.data.AppSettings
import com.bizane.app.data.FoodStorage
import com.bizane.app.data.L
import com.bizane.app.data.LocalTrashStorage
import com.bizane.app.ui.theme.CardBG
import com.bizane.app.ui.theme.PageBG

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: FoodViewModel,
    onOpenTrash: () -> Unit,
    onOpenAbout: () -> Unit = {},
    onOpenAccount: () -> Unit = {}
) {
    val context = LocalContext.current
    val notifOptions = listOf(1, 3, 7)
    var notifSelected by remember { mutableStateOf(notifOptions.indexOf(AppSettings.notifDays).let { if (it < 0) 1 else it }) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PageBG,
        topBar = {
            TopAppBar(
                title = { Text(L("tab.settings"), color = Color.White) },
                actions = {
                    TextButton(onClick = {
                        AppSettings.notifDays = notifOptions[notifSelected]
                        vm.refreshAfterEdit()
                    }) { Text(L("common.save"), color = Color.White, fontWeight = FontWeight.Bold) }
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
            SectionHeader(L("settings.notifSection"))
            Card {
                Text(L("settings.notifQuestion"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    notifOptions.forEachIndexed { i, days ->
                        SegmentedButton(
                            selected = notifSelected == i,
                            onClick = { notifSelected = i },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = notifOptions.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Color(0xFF0A84FF),
                                activeContentColor = Color.White,
                                inactiveContainerColor = CardBG,
                                inactiveContentColor = Color.White
                            )
                        ) { Text(L("settings.notifDaysOpt", days)) }
                    }
                }
            }

            // زمان
            Spacer(Modifier.height(20.dp))
            SectionHeader(L("settings.languageSection"))
            Card {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().clickable { showLanguagePicker = true }
                ) {
                    Text(AppLang.current.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text("🌐", fontSize = 18.sp)
                }
            }

            // هەژمار و باکئەپ
            Spacer(Modifier.height(20.dp))
            SectionHeader(L("settings.accountSection"))
            Card {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().clickable { onOpenAccount() }
                ) {
                    Column {
                        Text(L("settings.accountBackupBtn"), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        val linked = com.bizane.app.data.DriveTokenStore.isLinked
                        Text(
                            if (linked) L("settings.accountRowSubtitleLinked", com.bizane.app.data.DriveTokenStore.accountEmail ?: "")
                            else L("settings.accountRowSubtitleUnlinked"),
                            color = if (linked) Color(0xFF33D976) else Color(0xFFFF9500), fontSize = 12.sp
                        )
                    }
                    Text("›", color = Color.Gray, fontSize = 20.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader(L("settings.statsSection"))
            StatsCard()

            // سڕاوەکان — دوگمەیەکی زیندوو کە پەڕەیەکی تایبەت دەکاتەوە، لەگەڵ وێنە و گەڕاندنەوە
            Spacer(Modifier.height(20.dp))
            SectionHeader(L("settings.trashSection"))
            val trashCount = LocalTrashStorage.entries.size
            FilledActionButton(
                if (trashCount > 0) "🧹  ${L("settings.trashSection").trim()}  ·  $trashCount" else L("settings.trashSection"),
                color = Color(0xFF3378FA)
            ) { onOpenTrash() }

            Spacer(Modifier.height(20.dp))
            SectionHeader(L("settings.aboutContactSection"))
            Card {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().clickable { onOpenAbout() }
                ) {
                    Column {
                        Text(L("settings.aboutContactBtn"), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(L("settings.aboutRowSubtitle"), color = Color.Gray, fontSize = 12.sp)
                    }
                    Text("›", color = Color.Gray, fontSize = 20.sp)
                }
            }

            // داتا (مەترسیدارە، لەبەرئەوە لە خوارەوەی هەمووی دانراوە)
            Spacer(Modifier.height(20.dp))
            SectionHeader(L("settings.dataSection"))
            ActionButton(L("settings.clearAll"), color = Color(0xFFFF3B30)) { showClearConfirm = true }

            Spacer(Modifier.height(30.dp))
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(L("common.areYouSure")) },
            text = { Text(L("settings.clearAllMsg")) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    FoodStorage.items.toList().forEach { FoodStorage.delete(it.id) }
                    vm.refreshAfterEdit()
                }) { Text(L("settings.yesDeleteAll"), color = Color(0xFFFF3B30)) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text(L("common.no")) } }
        )
    }

    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text(L("settings.languageQuestion")) },
            text = {
                Column {
                    AppLanguage.values().forEach { lang ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppLang.current = lang
                                    showLanguagePicker = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(if (AppLang.current == lang) "●  " else "○  ")
                            Text(lang.title, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePicker = false }) { Text(L("common.close")) }
            }
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

/** دوگمەیەکی زیندووتر بۆ کردارە گرنگەکان — پاسدەخراوی ڕەنگاوڕەنگ، تاوەکو زۆر لە پێش چاو بێت */
@Composable
private fun FilledActionButton(title: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            StatItem("${items.size}", L("stats.total"), Color.White)
            StatItem("$ok", L("stats.ok"), Color(0xFF33D976))
            StatItem("$soon", L("stats.soon"), Color(0xFFFF9500))
            StatItem("$expired", L("stats.expired"), Color(0xFFFF3B30))
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
