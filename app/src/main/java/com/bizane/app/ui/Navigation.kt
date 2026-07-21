package com.bizane.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bizane.app.data.AppSettings
import com.bizane.app.data.FoodItem
import com.bizane.app.ui.theme.PageBG

private object Routes {
    const val MAIN = "main"
    const val ADD_ITEM = "addItem"
    const val EDIT_ITEM = "editItem"
    const val GROUP = "group"
    const val GROUP_MEMBERS = "groupMembers"
    const val TRASH = "trash"
    const val SETTINGS = "settings"
}

/** ئایتمی هەڵبژێردراو بۆ دەستکاریکردن، لەنێوان MainScreen و AddItemScreen ـدا هاوبەشکراوە */
private var editingItemHolder: FoodItem? = null

private enum class RootTab { HOME, SETTINGS }

/** پەڕەی سەرەکی لەگەڵ خزمەتگوزاری تابی خوارەوە: خواردنەکان و ڕێکخستنەکان وەک دوو تابی سەربەخۆ */
@Composable
private fun RootTabsScreen(
    vm: FoodViewModel,
    onOpenItem: (FoodItem?) -> Unit,
    onOpenGroup: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(RootTab.HOME) }

    Scaffold(
        containerColor = PageBG,
        bottomBar = {
            // تابی خوارەوە بە شێوەی "شناوگە"ی گڵۆپاوی وەک وەشانی ئایفۆن — کەمێک بۆشایی
            // لە هەردوو لاو خوارەوە، گۆشە خڕ و شێوەیەکی سەربەخۆ نەک تەواوی پانی شاشە
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1C1C1E))
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    modifier = Modifier.height(64.dp)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == RootTab.HOME,
                        onClick = { selectedTab = RootTab.HOME },
                        icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                        label = { Text("سەرەکی") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = Color(0xFF0A84FF),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == RootTab.SETTINGS,
                        onClick = { selectedTab = RootTab.SETTINGS },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("ڕێکخستنەکان") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = Color(0xFF0A84FF),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                RootTab.HOME -> MainScreen(vm = vm, onOpenItem = onOpenItem)
                RootTab.SETTINGS -> SettingsScreen(vm = vm, onOpenGroup = onOpenGroup)
            }
        }
    }
}

@Composable
fun BizaneNavHost() {
    val navController = rememberNavController()
    val vm: FoodViewModel = viewModel()

    LaunchedEffect(Unit) {
        vm.startPollingIfNeeded()
    }

    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            RootTabsScreen(
                vm = vm,
                onOpenItem = { item ->
                    editingItemHolder = item
                    navController.navigate(if (item == null) Routes.ADD_ITEM else Routes.EDIT_ITEM)
                },
                onOpenGroup = { navController.navigate(Routes.GROUP) }
            )
        }
        composable(Routes.ADD_ITEM) {
            AddItemScreen(
                vm = vm,
                editItem = null,
                groupId = if (AppSettings.groupId.isNotEmpty()) AppSettings.groupId else null,
                onClose = { navController.popBackStack() }
            )
        }
        composable(Routes.EDIT_ITEM) {
            AddItemScreen(
                vm = vm,
                editItem = editingItemHolder,
                groupId = if (AppSettings.groupId.isNotEmpty()) AppSettings.groupId else null,
                onClose = { navController.popBackStack() }
            )
        }
        composable(Routes.GROUP) {
            GroupScreen(
                vm = vm,
                onClose = { navController.popBackStack() },
                onOpenMembers = { navController.navigate(Routes.GROUP_MEMBERS) },
                onOpenTrash = { navController.navigate(Routes.TRASH) }
            )
        }
        composable(Routes.GROUP_MEMBERS) {
            GroupMembersScreen(onClose = { navController.popBackStack() })
        }
        composable(Routes.TRASH) {
            TrashScreen(onClose = { navController.popBackStack() })
        }
    }
}
