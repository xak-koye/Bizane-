package com.bizane.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizane.app.data.AppSettings
import com.bizane.app.data.FoodCategory
import com.bizane.app.data.FoodItem
import com.bizane.app.data.GroupService
import com.bizane.app.ui.theme.CardBG
import com.bizane.app.ui.theme.FieldBG
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: FoodViewModel,
    onOpenItem: (FoodItem?) -> Unit,
    onOpenSettings: () -> Unit
) {
    val items = vm.visibleItems
    val isCard = AppSettings.isCardView
    var cardToggle by remember { mutableStateOf(isCard) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLockDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(vm.wasKicked.value) {
        if (vm.wasKicked.value) {
            snackbarHostState.showSnackbar("ئەدمین لە گروپەکە دەریکردیت", duration = androidx.compose.material3.SnackbarDuration.Long)
            vm.consumeKicked()
        }
    }

    Scaffold(
        containerColor = com.bizane.app.ui.theme.PageBG,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = Color(0xFF262626),
                    contentColor = Color.White,
                    actionColor = Color(0xFFFF9500),
                    snackbarData = data
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(14.dp))

            // Title row
            Row(verticalAlignment = Alignment.CenterVertically) {
                val name = AppSettings.userName
                Text(
                    if (name.isEmpty()) "خواردنەکانم" else "سڵاو، $name!",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("${items.size}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                if (vm.isGrouped.value) {
                    IconCircleButton(
                        imageVector = if (vm.deleteUnlockedState.value) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        tint = if (vm.deleteUnlockedState.value) Color(0xFFFF3B30) else Color.White
                    ) {
                        if (vm.deleteUnlockedState.value) {
                            vm.setDeleteUnlocked(false)
                        } else {
                            showLockDialog = true
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
                IconCircleButton(imageVector = Icons.Filled.Settings, onClick = onOpenSettings)
                Spacer(Modifier.width(8.dp))
                IconCircleButton(imageVector = if (cardToggle) Icons.Filled.ViewList else Icons.Filled.GridView) {
                    cardToggle = vm.toggleCardView()
                }
                Spacer(Modifier.width(8.dp))
                IconCircleButton(imageVector = Icons.Filled.Add) { onOpenItem(null) }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                statusText(vm),
                color = Color.Gray, fontSize = 12.sp
            )

            Spacer(Modifier.height(10.dp))
            // Search + sort
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = vm.searchQuery.value,
                    onValueChange = { vm.setSearch(it) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    placeholder = { Text("🔍  گەڕان بەدوای خواردندا...", color = Color.Gray, fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FieldBG, unfocusedContainerColor = FieldBG,
                        disabledContainerColor = FieldBG,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    IconCircleButton(imageVector = Icons.Filled.SwapVert) { showSortMenu = true }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        SortMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text((if (mode == vm.sortMode.value) "✓  " else "") + mode.title)
                                },
                                onClick = { vm.setSortMode(mode); showSortMenu = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            // Category chips
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(FoodCategory.values().toList()) { cat ->
                    CategoryChip(cat, selected = cat == vm.selectedCategory.value) { vm.setCategory(cat) }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (vm.searchQuery.value.isBlank()) "🛒\n\nهیچ خواردنێک نییە\nبستێنە + بۆ زیادکردن"
                        else "🔍\n\nهیچ ئەنجامێک نەدۆزرایەوە",
                        color = Color.Gray, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else if (cardToggle) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        FoodCard(item, onClick = { onOpenItem(item) })
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        SwipeableFoodRow(
                            item = item,
                            canModify = vm.canModify(item),
                            onClick = { onOpenItem(item) },
                            onDeleteConfirmed = {
                                vm.deleteItem(item)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "\"${item.name}\" سڕایەوە",
                                        actionLabel = "گەڕاندنەوە",
                                        duration = androidx.compose.material3.SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) vm.undoDelete()
                                    else vm.clearPendingUndo()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showLockDialog) {
        LockUnlockDialog(
            onDismiss = { showLockDialog = false },
            onConfirm = { user, pass, callback ->
                GroupService.fetchDeleteCredentials(AppSettings.groupId) { savedUser, savedHash ->
                    val ok = !savedUser.isNullOrEmpty() && !savedHash.isNullOrEmpty() &&
                        user == savedUser && GroupService.sha256(pass) == savedHash
                    if (ok) {
                        vm.setDeleteUnlocked(true)
                        showLockDialog = false
                    }
                    callback(ok)
                }
            }
        )
    }
}

private fun statusText(vm: FoodViewModel): String {
    return if (vm.isGrouped.value) {
        val t = vm.lastSyncTime.value
        if (t != null) {
            val f = SimpleDateFormat("HH:mm", Locale.getDefault())
            "🔄 دوایین نوێکردنەوە: ${f.format(Date(t))}"
        } else "🔄 نوێکردنەوە..."
    } else "📱 کۆگای کەسی"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableFoodRow(
    item: FoodItem,
    canModify: Boolean,
    onClick: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("دڵنیایت؟") },
            text = { Text("ئایا دەتەوێت \"${item.name}\" بسڕیتەوە؟ ئەم کارە ناگەڕێتەوە.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onDeleteConfirmed() }) {
                    Text("بەڵێ، بسڕەوە", color = Color(0xFFFF3B30))
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("نەخێر") } }
        )
    }

    if (!canModify) {
        FoodListRow(item, onClick = onClick)
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                showConfirm = true
            }
            false // ناهێڵین خۆکارانە لاببرێت، سڕینەوەی ڕاستەقینە پاش دڵنیایی دەکرێت
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFF3B30)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text("🗑 سڕینەوە", color = Color.White, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    ) {
        FoodListRow(item, onClick = onClick)
    }
}

@Composable
private fun LockUnlockDialog(onDismiss: () -> Unit, onConfirm: (String, String, (Boolean) -> Unit) -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("کردنەوەی دەسەڵاتی سڕینەوە") },
        text = {
            Column {
                Text(
                    "یوزەرنەیم و وشەی نهێنی بنووسە تاوەکو هەموو ئەندامان بتوانن هەر ئایتمێک بسڕنەوە.",
                    fontSize = 13.sp, color = Color.Gray
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = user, onValueChange = { user = it; error = false },
                    label = { Text("یوزەرنەیم") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pass, onValueChange = { pass = it; error = false },
                    label = { Text("وشەی نهێنی") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error) {
                    Spacer(Modifier.height(6.dp))
                    Text("یوزەر یان وشەی نهێنی هەڵەیە", color = Color(0xFFFF3B30), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(user, pass) { ok -> error = !ok } }) { Text("کردنەوە") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("پاشگەزبوونەوە") } }
    )
}
