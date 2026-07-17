package com.bizane.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.bizane.app.data.AppSettings
import com.bizane.app.data.AuthManager
import com.bizane.app.data.FoodCategory
import com.bizane.app.data.FoodItem
import com.bizane.app.data.FoodStorage
import com.bizane.app.data.FoodSyncService
import com.bizane.app.data.GroupService

enum class SortMode(val title: String) {
    EXPIRY_SOONEST("نزیکترین بەسەرچوون"),
    EXPIRY_LATEST("دوورترین بەسەرچوون"),
    NAME_AZ("ناو (ئا-یی)");

    companion object {
        fun fromIndex(i: Int) = values().getOrElse(i) { EXPIRY_SOONEST }
    }
}

/** ViewModel ـی هاوبەش لەنێوان هەموو پەڕەکان، وەکو ئەو state ـەی ViewController.swift هەڵیدەگرت */
class FoodViewModel : ViewModel() {

    var groupItemsRaw = mutableStateOf<List<FoodItem>>(emptyList())
        private set
    var isGrouped = mutableStateOf(AppSettings.groupId.isNotEmpty())
        private set
    var lastSyncTime = mutableStateOf<Long?>(null)
        private set
    var localRefreshTick = mutableStateOf(0)
        private set
    var deleteUnlockedState = mutableStateOf(AppSettings.deleteUnlocked)
        private set
    var wasKicked = mutableStateOf(false)
        private set

    var selectedCategory = mutableStateOf(FoodCategory.ALL)
    var searchQuery = mutableStateOf("")
    var sortMode = mutableStateOf(SortMode.fromIndex(AppSettings.sortMode))
    var pendingUndoItem = mutableStateOf<FoodItem?>(null)
        private set

    /** لیستی فلتەرکراو/ڕیزکراوی ئایتمەکان — بەپێی state ـی سەرەوە */
    val visibleItems: List<FoodItem>
        get() {
            val source: List<FoodItem> = if (isGrouped.value) {
                if (AppSettings.isGroupAdmin) groupItemsRaw.value
                else {
                    val uid = AuthManager.uid
                    groupItemsRaw.value.filter { it.ownerId == null || it.ownerId == uid }
                }
            } else {
                localRefreshTick.value // dependency بۆ recomposition
                FoodStorage.items
            }
            var list = when (sortMode.value) {
                SortMode.EXPIRY_SOONEST -> source.sortedBy { it.expiryDate }
                SortMode.EXPIRY_LATEST -> source.sortedByDescending { it.expiryDate }
                SortMode.NAME_AZ -> source.sortedBy { it.name.lowercase() }
            }
            if (selectedCategory.value != FoodCategory.ALL) {
                list = list.filter { it.category == selectedCategory.value }
            }
            val q = searchQuery.value.trim()
            if (q.isNotEmpty()) list = list.filter { it.name.contains(q, ignoreCase = true) }
            return list
        }

    fun refreshLocal() { localRefreshTick.value++ }

    fun startPollingIfNeeded() {
        isGrouped.value = AppSettings.groupId.isNotEmpty()
        if (isGrouped.value) {
            FoodSyncService.startPolling(
                AppSettings.groupId,
                onUpdate = { fetched ->
                    groupItemsRaw.value = fetched
                    lastSyncTime.value = System.currentTimeMillis()
                    deleteUnlockedState.value = AppSettings.deleteUnlocked
                },
                onKicked = {
                    FoodSyncService.stopPolling()
                    AppSettings.clearGroup()
                    isGrouped.value = false
                    groupItemsRaw.value = emptyList()
                    wasKicked.value = true
                }
            )
        } else {
            FoodSyncService.stopPolling()
        }
    }

    fun refreshAfterEdit() {
        isGrouped.value = AppSettings.groupId.isNotEmpty()
        if (isGrouped.value) {
            FoodSyncService.fetchItems(AppSettings.groupId) { fetched ->
                groupItemsRaw.value = fetched
                lastSyncTime.value = System.currentTimeMillis()
            }
        } else {
            refreshLocal()
        }
    }

    fun canModify(item: FoodItem): Boolean {
        if (!isGrouped.value) return true
        if (AppSettings.deleteUnlocked) return true
        val isOwnItem = item.ownerId == null || item.ownerId == AuthManager.uid
        return isOwnItem && AppSettings.canDeleteOwnItems
    }

    fun consumeKicked() { wasKicked.value = false }

    fun deleteItem(item: FoodItem) {
        if (isGrouped.value) {
            item.firestoreId?.let { fid ->
                val myName = AppSettings.userName.ifEmpty { "ئەندام" }
                FoodSyncService.delete(AppSettings.groupId, item, myName)
                groupItemsRaw.value = groupItemsRaw.value.filter { it.firestoreId != fid }
            }
        } else {
            FoodStorage.delete(item.id)
            refreshLocal()
        }
        pendingUndoItem.value = item
    }

    fun undoDelete() {
        val item = pendingUndoItem.value ?: return
        pendingUndoItem.value = null
        if (isGrouped.value) {
            val copy = item.copy(firestoreId = null)
            FoodSyncService.save(AppSettings.groupId, copy) { refreshAfterEdit() }
        } else {
            FoodStorage.add(item)
            refreshLocal()
        }
    }

    fun clearPendingUndo() { pendingUndoItem.value = null }

    fun setDeleteUnlocked(unlocked: Boolean) {
        AppSettings.deleteUnlocked = unlocked
        deleteUnlockedState.value = unlocked
        if (isGrouped.value) GroupService.setDeleteUnlocked(AppSettings.groupId, unlocked)
    }

    fun setSortMode(mode: SortMode) {
        sortMode.value = mode
        AppSettings.sortMode = SortMode.values().indexOf(mode)
    }

    fun setCategory(cat: FoodCategory) { selectedCategory.value = cat }
    fun setSearch(q: String) { searchQuery.value = q }

    fun toggleCardView(): Boolean {
        AppSettings.isCardView = !AppSettings.isCardView
        return AppSettings.isCardView
    }
}
