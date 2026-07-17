package com.bizane.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizane.app.data.AppSettings
import com.bizane.app.data.AuthManager
import com.bizane.app.data.FoodStorage
import com.bizane.app.data.FoodSyncService
import com.bizane.app.data.GroupService
import com.bizane.app.ui.theme.CardBG
import com.bizane.app.ui.theme.FieldBG
import com.bizane.app.ui.theme.PageBG

/** ئایتمە کۆنەکان (پێش گروپ) دەنێرێتە ناو گروپەکە، تاوەکو لەناونەچن و ئەدمینیش ببینێت */
private fun migrateLocalItemsIntoGroup(groupId: String, onDone: () -> Unit = {}) {
    val localItems = FoodStorage.items.filter { it.firestoreId == null }.toList()
    if (localItems.isEmpty()) { onDone(); return }
    val myUid = AuthManager.uid
    val myName = AppSettings.userName.ifEmpty { "ئەندام" }
    var remaining = localItems.size
    localItems.forEach { item ->
        val toSave = item.copy(ownerId = myUid, ownerName = myName)
        FoodSyncService.save(groupId, toSave) {
            remaining--
            if (remaining == 0) {
                localItems.forEach { FoodStorage.delete(it.id) }
                onDone()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    vm: FoodViewModel,
    onClose: () -> Unit,
    onOpenMembers: () -> Unit,
    onOpenTrash: () -> Unit
) {
    var hasGroup by remember { mutableStateOf(AppSettings.groupId.isNotEmpty()) }

    Scaffold(
        containerColor = PageBG,
        topBar = {
            TopAppBar(
                title = { Text("گروپی هاوبەش", color = Color.White) },
                navigationIcon = { TextButton(onClick = onClose) { Text("داخستن", color = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBG)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (hasGroup) {
                CurrentGroupView(
                    onOpenMembers = onOpenMembers,
                    onOpenTrash = onOpenTrash,
                    onLeft = { hasGroup = false; vm.startPollingIfNeeded(); vm.refreshAfterEdit() }
                )
            } else {
                JoinCreateView(onGroupJoined = {
                    hasGroup = true
                    vm.startPollingIfNeeded()
                    vm.refreshAfterEdit()
                })
            }
        }
    }
}

@Composable
private fun CurrentGroupView(onOpenMembers: () -> Unit, onOpenTrash: () -> Unit, onLeft: () -> Unit) {
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showMemberLeaveCreds by remember { mutableStateOf(false) }
    var showChangeCreds by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBG)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "🔗 ${AppSettings.groupName}" + (if (AppSettings.isGroupAdmin) "  🛡️ ئەدمین" else ""),
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
                )
                Spacer(Modifier.height(6.dp))
                Text("کۆدی بانگهێشت: ${AppSettings.groupCode}", color = Color.Gray, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "ئەم کۆدە بدە بە ئەندامانی خێزان/ماڵەوە تاوەکو بتوانن بەشداری بکەن.",
            color = Color.Gray, fontSize = 12.sp
        )

        if (AppSettings.isGroupAdmin) {
            Spacer(Modifier.height(18.dp))
            OutlinedButton(
                onClick = onOpenMembers,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBG, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) { Text("👥  بینینی ئەندامان") }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenTrash,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBG, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) { Text("🗑️  تەنەکەی خۆڵ") }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showChangeCreds = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBG, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) { Text("🔑  گۆڕینی یوزەر/پاسی سڕینەوە") }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                if (AppSettings.isGroupAdmin) showLeaveConfirm = true else showMemberLeaveCreds = true
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBG, contentColor = Color(0xFFFF3B30)),
            shape = RoundedCornerShape(14.dp)
        ) { Text(if (AppSettings.isGroupAdmin) "کۆتایی هێنان بە گروپ" else "دەرچوون لە گروپ") }
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("دڵنیایی؟") },
            text = { Text("تۆ ئەدمینی گروپیت. ئەگەر جیابیتەوە، گروپەکە کۆتایی دێت و هەموو ئەندامانی تریش لە گروپ دەردەچن.") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveConfirm = false
                    val groupId = AppSettings.groupId
                    GroupService.endGroup(groupId) {
                        AppSettings.clearGroup()
                        AppSettings.canEditGroup = true
                        AppSettings.canDeleteOwnItems = true
                        FoodSyncService.stopPolling()
                        onLeft()
                    }
                }) { Text("بەڵێ، گروپ کۆتایی پێبهێنە", color = Color(0xFFFF3B30)) }
            },
            dismissButton = { TextButton(onClick = { showLeaveConfirm = false }) { Text("نەخێر") } }
        )
    }

    if (showMemberLeaveCreds) {
        MemberLeaveCredentialsDialog(
            onDismiss = { showMemberLeaveCreds = false },
            onConfirm = { user, pass, onWrong ->
                val groupId = AppSettings.groupId
                GroupService.fetchDeleteCredentials(groupId) { savedUser, savedHash ->
                    if (savedUser.isNullOrEmpty() || savedHash.isNullOrEmpty() ||
                        user != savedUser || GroupService.sha256(pass) != savedHash
                    ) {
                        onWrong()
                    } else {
                        showMemberLeaveCreds = false
                        GroupService.leaveGroup(groupId) {
                            AppSettings.clearGroup()
                            AppSettings.canEditGroup = true
                            AppSettings.canDeleteOwnItems = true
                            FoodSyncService.stopPolling()
                            onLeft()
                        }
                    }
                }
            }
        )
    }

    if (showChangeCreds) {
        SetDeleteCredentialsDialog(
            onSkip = { showChangeCreds = false },
            onSave = { user, pass ->
                GroupService.setDeleteCredentials(AppSettings.groupId, user, pass)
                showChangeCreds = false
            }
        )
    }
}

/** ئەندامی ئاسایی خۆی لە گروپ دەردەبات، دوای پشکنینی یوزەر/پاسی دەسەڵاتی گروپەکە */
@Composable
private fun MemberLeaveCredentialsDialog(onDismiss: () -> Unit, onConfirm: (String, String, () -> Unit) -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("دەرچوون لە گروپ") },
        text = {
            Column {
                Text("یوزەر و وشەی نهێنی دەسەڵاتی سڕینەوەی گروپەکە بنووسە تاوەکو دەربچیت.", color = Color.Gray, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                GroupTextField(user, "یوزەرنەیم") { user = it }
                Spacer(Modifier.height(8.dp))
                GroupTextField(pass, "وشەی نهێنی") { pass = it }
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = Color(0xFFFF3B30), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(user.trim(), pass) { error = "یوزەر یان وشەی نهێنی هەڵەیە" }
            }) { Text("دەرچوون", color = Color(0xFFFF3B30)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("پاشگەزبوونەوە") } }
    )
}

@Composable
private fun JoinCreateView(onGroupJoined: () -> Unit) {
    var yourName by remember { mutableStateOf(AppSettings.userName) }
    var newGroupName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var pendingCredsGroupId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("👤  ناوی تۆ", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        GroupTextField(yourName, "ناوی تۆ (بۆ ناسینەوەت لەلایەن ئەندامانی تر)") { yourName = it }

        Spacer(Modifier.height(20.dp))
        Text("➕  دروستکردنی گروپی نوێ", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        GroupTextField(newGroupName, "ناوی گروپی نوێ") { newGroupName = it }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                val name = yourName.trim()
                if (name.isEmpty()) { errorMsg = "تکایە ناوی خۆت بنووسە"; return@Button }
                val gName = newGroupName.trim()
                if (gName.isEmpty()) { errorMsg = "ناوی گروپ بنووسە"; return@Button }
                AppSettings.userName = name
                loading = true; errorMsg = null
                GroupService.createGroup(gName) { groupId, code, ownerId, error ->
                    loading = false
                    if (groupId != null && code != null && ownerId != null) {
                        AppSettings.setGroup(groupId, gName, code, ownerId)
                        migrateLocalItemsIntoGroup(groupId)
                        successMsg = "گروپ دروستکرا! 🎉\nکۆدی بانگهێشت: $code"
                        pendingCredsGroupId = groupId
                    } else {
                        errorMsg = error ?: "شتێک هەڵەی ڕوویدا"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PageBG),
            shape = RoundedCornerShape(14.dp)
        ) { Text("دروستکردنی گروپ", fontWeight = FontWeight.SemiBold) }

        Spacer(Modifier.height(28.dp))
        Text("🔑  چوونە ناو گروپێکی هەبوو", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        GroupTextField(joinCode, "کۆدی گروپ (٦ پیت)") { joinCode = it }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                val name = yourName.trim()
                if (name.isEmpty()) { errorMsg = "تکایە ناوی خۆت بنووسە"; return@OutlinedButton }
                val code = joinCode.trim()
                if (code.isEmpty()) { errorMsg = "کۆدی گروپ بنووسە"; return@OutlinedButton }
                AppSettings.userName = name
                loading = true; errorMsg = null
                GroupService.joinGroup(code) { groupId, gName, ownerId, error ->
                    loading = false
                    if (groupId != null && gName != null && ownerId != null) {
                        AppSettings.setGroup(groupId, gName, code.uppercase(), ownerId)
                        migrateLocalItemsIntoGroup(groupId)
                        onGroupJoined()
                    } else {
                        errorMsg = error ?: "هیچ گروپێک بەم کۆدە نەدۆزرایەوە"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBG, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp)
        ) { Text("چوونە ناو گروپ", fontWeight = FontWeight.SemiBold) }

        if (loading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(color = Color.White)
        }
        errorMsg?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, color = Color(0xFFFF3B30), fontSize = 13.sp)
        }
        successMsg?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, color = Color(0xFF33D976), fontSize = 13.sp)
        }
    }

    pendingCredsGroupId?.let { groupId ->
        SetDeleteCredentialsDialog(
            onSkip = { pendingCredsGroupId = null; onGroupJoined() },
            onSave = { user, pass ->
                GroupService.setDeleteCredentials(groupId, user, pass)
                pendingCredsGroupId = null
                onGroupJoined()
            }
        )
    }
}

/** ئەدمین یوزەر/پاسی دەسەڵاتی سڕینەوە دادەنێت، وەکو promptSetDeleteCredentials ـی iOS */
@Composable
private fun SetDeleteCredentialsDialog(onSkip: () -> Unit, onSave: (String, String) -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("یوزەر و پاسی دەسەڵاتی سڕینەوە") },
        text = {
            Column {
                Text(
                    "ئەم یوزەر/پاسە دواتر بەکاردێت بۆ کردنەوەی دەسەڵاتی سڕینەوەی هەموو ئایتمەکان (دوگمەی 🔒)، " +
                        "هەروەها بۆ ئەندامان کاتێک دەیانەوێت لە گروپ دەربچن.",
                    color = Color.Gray, fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                GroupTextField(user, "یوزەرنەیم") { user = it }
                Spacer(Modifier.height(8.dp))
                GroupTextField(pass, "وشەی نهێنی") { pass = it }
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = Color(0xFFFF3B30), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (user.trim().isEmpty() || pass.isEmpty()) {
                    error = "پێویستە یوزەر و پاس هەردووکیان پڕبکرێنەوە"
                } else {
                    onSave(user.trim(), pass)
                }
            }) { Text("پاشەکەوتکردن") }
        },
        dismissButton = { TextButton(onClick = onSkip) { Text("دواتر (بەبێ ئەمە)") } }
    )
}

@Composable
private fun GroupTextField(value: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        placeholder = { Text(placeholder, color = Color.Gray, fontSize = 13.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = CardBG, unfocusedContainerColor = CardBG,
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
        )
    )
}
