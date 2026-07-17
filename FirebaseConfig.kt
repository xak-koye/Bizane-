package com.bizane.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bizane.app.data.AppSettings
import com.bizane.app.data.FoodItem

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

@Composable
fun BizaneNavHost() {
    val navController = rememberNavController()
    val vm: FoodViewModel = viewModel()

    LaunchedEffect(Unit) {
        vm.startPollingIfNeeded()
    }

    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                vm = vm,
                onOpenItem = { item ->
                    editingItemHolder = item
                    navController.navigate(if (item == null) Routes.ADD_ITEM else Routes.EDIT_ITEM)
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
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
        composable(Routes.SETTINGS) {
            SettingsScreen(
                vm = vm,
                onClose = { navController.popBackStack() },
                onOpenGroup = { navController.navigate(Routes.GROUP) }
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
