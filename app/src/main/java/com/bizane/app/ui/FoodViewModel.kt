package com.bizane.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.bizane.app.data.AppSettings
import com.bizane.app.data.FoodCategory
import com.bizane.app.data.FoodItem
import com.bizane.app.data.FoodStorage

enum class SortMode(val title: String) {
    EXPIRY_SOONEST("نزیکترین بەسەرچوون"),
    EXPIRY_LATEST("دوورترین بەسەرچوون"),
    NAME_AZ("ناو (ئا-یی)");

    companion object {
        fun fromIndex(i: Int) = values().getOrElse(i) { EXPIRY_SOONEST }
    }
}

/** ViewModel ـی هاوبەش لەنێوان هەموو پەڕەکان — کۆگای کەسی، ئۆفلاین بە تەواوی */
class FoodViewModel : ViewModel() {

    var localRefreshTick = mutableStateOf(0)
        private set

    var selectedCategory = mutableStateOf(FoodCategory.ALL)
    var searchQuery = mutableStateOf("")
    var sortMode = mutableStateOf(SortMode.fromIndex(AppSettings.sortMode))
    var pendingUndoItem = mutableStateOf<FoodItem?>(null)
        private set

    /** لیستی فلتەرکراو/ڕیزکراوی ئایتمەکان — بەپێی state ـی سەرەوە */
    val visibleItems: List<FoodItem>
        get() {
            localRefreshTick.value // dependency بۆ recomposition
            var list = when (sortMode.value) {
                SortMode.EXPIRY_SOONEST -> FoodStorage.items.sortedBy { it.expiryDate }
                SortMode.EXPIRY_LATEST -> FoodStorage.items.sortedByDescending { it.expiryDate }
                SortMode.NAME_AZ -> FoodStorage.items.sortedBy { it.name.lowercase() }
            }
            if (selectedCategory.value != FoodCategory.ALL) {
                list = list.filter { it.category == selectedCategory.value }
            }
            val q = searchQuery.value.trim()
            if (q.isNotEmpty()) list = list.filter { it.name.contains(q, ignoreCase = true) }
            return list
        }

    fun refreshLocal() { localRefreshTick.value++ }

    fun refreshAfterEdit() { refreshLocal() }

    fun deleteItem(item: FoodItem) {
        FoodStorage.delete(item.id) // خۆکار تۆمار دەکرێت لە خۆڵدا لەناو FoodStorage.delete
        refreshLocal()
        pendingUndoItem.value = item
    }

    fun undoDelete() {
        val item = pendingUndoItem.value ?: return
        pendingUndoItem.value = null
        FoodStorage.add(item)
        val trashIndex = com.bizane.app.data.LocalTrashStorage.entries.indexOfFirst { it.item.id == item.id }
        if (trashIndex >= 0) com.bizane.app.data.LocalTrashStorage.removeForever(trashIndex)
        refreshLocal()
    }

    fun clearPendingUndo() { pendingUndoItem.value = null }

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
