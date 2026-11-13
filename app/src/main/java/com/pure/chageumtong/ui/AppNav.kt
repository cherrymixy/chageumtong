package com.pure.chageumtong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pure.chageumtong.core.AppContainer
import com.pure.chageumtong.ui.components.BigButton
import com.pure.chageumtong.ui.components.GrayBox
import com.pure.chageumtong.ui.screens.analysis.AnalysisScreen
import com.pure.chageumtong.ui.screens.home.HomeScreen
import com.pure.chageumtong.ui.screens.map.MapScreen
import com.pure.chageumtong.ui.screens.shop.ShopScreen
import com.pure.chageumtong.ui.theme.Blue
import com.pure.chageumtong.ui.theme.BoxGray
import com.pure.chageumtong.ui.theme.CardBg
import com.pure.chageumtong.ui.theme.fig

private val TABS = listOf("home" to "홈", "map" to "지도", "shop" to "상점")

private fun NavController.navigateTab(route: String) = navigate(route) {
    popUpTo("home") { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AppRoot(container: AppContainer, modifier: Modifier = Modifier) {
    val vm: AppViewModel = viewModel { AppViewModel(container) }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    Column(modifier.fillMaxSize()) {
        NavHost(nav, startDestination = "home", modifier = Modifier.weight(1f)) {
            composable("home") {
                HomeScreen(
                    vm,
                    onClaimed = { id -> nav.navigate("analysis/$id") },
                    onShop = { nav.navigateTab("shop") },
                    onHistory = { nav.navigate("history") },
                )
            }
            composable(
                "analysis/{id}?readonly={readonly}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("readonly") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { e ->
                val readonly = e.arguments?.getBoolean("readonly") ?: false
                AnalysisScreen(vm, e.arguments?.getString("id").orEmpty(), readonly) {
                    if (readonly) nav.popBackStack() else nav.popBackStack("home", inclusive = false)
                }
            }
            composable("map") { MapScreen(vm) }
            composable("shop") { ShopScreen(vm) }
            composable("history") { GrayBox("적립 내역 (준비 중)", Modifier.fillMaxSize()) }
        }
        // 좌하단 플로팅 탭바: 홈/지도/상점에서만
        if (route in TABS.map { it.first }) {
            Row(
                Modifier.padding(start = fig(24), bottom = fig(16)).background(CardBg, RoundedCornerShape(fig(24))).padding(fig(8)),
                horizontalArrangement = Arrangement.spacedBy(fig(8)),
            ) {
                TABS.forEach { (r, label) ->
                    val selected = r == route
                    BigButton(
                        label,
                        { nav.navigateTab(r) },
                        Modifier.width(fig(140)),
                        container = if (selected) Blue else BoxGray,
                        content = if (selected) Color.White else Color.Black,
                    )
                }
            }
        }
    }
}
