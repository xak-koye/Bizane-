package com.bizane.app.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizane.app.data.AppSettings
import com.bizane.app.data.AuthManager
import com.bizane.app.data.GroupService
import com.bizane.app.ui.theme.CardBG
import com.bizane.app.ui.theme.PageBG

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersScreen(onClose: () -> Unit) {
    var members by remember { mutableStateOf<List<GroupService.Member>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errored by remember { mutableStateOf(false) }
    var reloadTick by remember { mutableStateOf(0) }
    var selectedMember by remember { mutableStateOf<GroupService.Member?>(null) }

    fun reload() {
        loading = true; errored = false
        GroupService.fetchMembers(AppSettings.groupId) { result ->
            loading = false
            members = result
            // خۆت (وەکو ئەدمین) هەمیشە دەبێت لە لیستەکەدا بیت؛ ئەگەر بەتاڵ گەڕایەوە زۆرترین ئەگەر هەڵەی تۆڕە
            errored = result.isEmpty()
        }
    }

    LaunchedEffect(reloadTick) { reload() }

    Scaffold(
        containerColor = PageBG,
        topBar = {
            TopAppBar(
                title = { Text("ئەندامانی گروپ", color = Color.White) },
                navigationIcon = { TextButton(onClick = onClose) { Text("داخستن", color = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBG)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
                errored -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "نەتوانرا ئەندامان بار بکرێن.\nتکایە هێڵی ئینتەرنێت بپشکنە و دووبارە هەوڵبدەرەوە.",
                        color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { reloadTick++ },
                        colors = ButtonDefaults.buttonColors(containerColor = CardBG, contentColor = Color.White)
                    ) { Text("دووبارە هەوڵبدەرەوە") }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members, key = { it.uid }) { m ->
                        val clickable = m.uid != AuthManager.uid && !m.isOwner
                        var text = "👤  ${m.name}"
                        if (m.isOwner) {
                            text += "  🛡️"
                        } else {
                            if (!m.canEdit) text += "  👁️ تەنیا بینین"
                            if (!m.canDelete) text += "  🔒 ناتوانێت بسڕێتەوە"
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardBG)
                                .clickable(enabled = clickable) { selectedMember = m }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(text, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    selectedMember?.let { m ->
        AlertDialog(
            onDismissRequest = { selectedMember = null },
            title = { Text(m.name) },
            text = { Text("چی دەتەوێت بۆ ئەم ئەندامە بکەیت؟") },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        GroupService.setMemberPermission(AppSettings.groupId, m.uid, !m.canEdit) { reloadTick++ }
                        selectedMember = null
                    }) {
                        Text(if (m.canEdit) "بیکە بە تەنیا-بینین" else "ڕێگەی زیادکردنی بدەرەوە")
                    }
                    TextButton(onClick = {
                        GroupService.setMemberDeletePermission(AppSettings.groupId, m.uid, !m.canDelete) { reloadTick++ }
                        selectedMember = null
                    }) {
                        Text(if (m.canDelete) "قەدەغەکردنی سڕینەوە" else "ڕێگەی سڕینەوەی بدەرەوە")
                    }
                    TextButton(onClick = {
                        GroupService.removeMember(AppSettings.groupId, m.uid) { reloadTick++ }
                        selectedMember = null
                    }) { Text("لابردن لە گروپ", color = Color(0xFFFF3B30)) }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMember = null }) { Text("پاشگەزبوونەوە") }
            }
        )
    }
}
