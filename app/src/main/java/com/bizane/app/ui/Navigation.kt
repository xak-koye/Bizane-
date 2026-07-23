package com.bizane.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bizane.app.data.FoodItem
import com.bizane.app.ui.theme.IconBtnBG
import com.bizane.app.ui.theme.PageBG

private object Routes {
    const val MAIN = "main"
    const val ADD_ITEM = "addItem"
    const val EDIT_ITEM = "editItem"
    const val TRASH = "trash"
    const val SETTINGS = "settings"
}

/** ئایتمی هەڵبژێردراو بۆ دەستکاریکردن، لەنێوان MainScreen و AddItemScreen ـدا هاوبەشکراوە */
private var editingItemHolder: FoodItem? = null

@Composable
fun BizaneNavHost() {
    val navController = rememberNavController()
    val vm: FoodViewModel = viewModel()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // تابی خوارەوە تەنیا لەسەر دوو پەڕەی سەرەکی دەردەکەوێت (سەرەکی/ڕێکخستنەکان)،
    // وەکو ئەپی iOS — لە کاتی زیادکردن/دەستکاری/سڕاوەکان شاردراوەتەوە چونکە ئەوانە پەڕەی لاوەکین
    val showBottomBar = currentRoute == Routes.MAIN || currentRoute == Routes.SETTINGS

    Scaffold(
        containerColor = PageBG,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = Color(0xFF1C1C22)) {
                    NavigationBarItem(
                        selected = currentRoute == Routes.MAIN,
                        onClick = {
                            navController.navigate(Routes.MAIN) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { androidx.compose.material3.Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                        label = { Text("سەرەکی") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White, selectedTextColor = Color.White,
                            unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray,
                            indicatorColor = IconBtnBG
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = {
                            navController.navigate(Routes.SETTINGS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { androidx.compose.material3.Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("رێکخستنەکان") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White, selectedTextColor = Color.White,
                            unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray,
                            indicatorColor = IconBtnBG
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MAIN,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(Routes.MAIN) {
                MainScreen(
                    vm = vm,
                    onOpenItem = { item ->
                        editingItemHolder = item
                        navController.navigate(if (item == null) Routes.ADD_ITEM else Routes.EDIT_ITEM)
                    }
                )
            }
            composable(Routes.ADD_ITEM) {
                AddItemScreen(
                    vm = vm,
                    editItem = null,
                    onClose = { navController.popBackStack() }
                )
            }
            composable(Routes.EDIT_ITEM) {
                AddItemScreen(
                    vm = vm,
                    editItem = editingItemHolder,
                    onClose = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    vm = vm,
                    onOpenTrash = { navController.navigate(Routes.TRASH) }
                )
            }
            composable(Routes.TRASH) {
                TrashScreen(
                    onClose = { navController.popBackStack() },
                    onRestore = { vm.refreshAfterEdit() }
                )
            }
        }
    }
}
