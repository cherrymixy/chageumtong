package com.pure.chageumtong.ui

import androidx.compose.foundation.background
import com.pure.chageumtong.ui.theme.TextStat
import com.pure.chageumtong.ui.theme.Surface
import com.pure.chageumtong.ui.theme.StrokeGray
import com.pure.chageumtong.ui.theme.MIN_TOUCH
import com.pure.chageumtong.ui.theme.D
import com.pure.chageumtong.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pure.chageumtong.core.AppContainer
import com.pure.chageumtong.ui.components.DriveAlertHost
import com.pure.chageumtong.ui.screens.analysis.AnalysisScreen
import com.pure.chageumtong.ui.screens.history.HistoryScreen
import com.pure.chageumtong.ui.screens.home.HomeScreen
import com.pure.chageumtong.ui.screens.map.MapScreen
import com.pure.chageumtong.ui.screens.my.MyScreen
import com.pure.chageumtong.ui.screens.my.MyTabIcon
import com.pure.chageumtong.ui.screens.shop.ShopScreen
import com.pure.chageumtong.ui.theme.fig

private val TABS = listOf("home" to "홈", "map" to "지도", "shop" to "상점", "my" to "마이")
private val TAB_ICONS = mapOf("home" to R.drawable.ic_tab_home, "map" to R.drawable.ic_tab_map, "shop" to R.drawable.ic_tab_shop)

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

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
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
                composable("my") { MyScreen(vm, onHistory = { nav.navigate("history") }) }
                composable("history") {
                    HistoryScreen(vm, onOpenSession = { id -> nav.navigate("analysis/$id?readonly=true") }, onBack = { nav.popBackStack() })
                }
            }
            // 좌하단 플로팅 탭바 (Figma 95:2211): 342×85, 아이콘 40, 선택 = 진한 색. 마이 탭 추가로 폭 342 → 446 (Figma 없음)
            if (route in TABS.map { it.first }) {
                Row(
                    Modifier.padding(start = D.ScreenPad, top = D.BottomGap, bottom = fig(26))
                        .size(fig(446), D.BottomBarHeight)
                        .clip(RoundedCornerShape(D.RadiusCard)).background(Surface)
                        .padding(horizontal = fig(30)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TABS.forEach { (r, label) ->
                        Box(
                            Modifier.size(MIN_TOUCH).clickable { nav.navigateTab(r) },
                            contentAlignment = Alignment.Center,
                        ) {
                            val tint = if (r == route) TextStat else StrokeGray
                            val icon = TAB_ICONS[r]
                            if (icon != null) {
                                Image(painterResource(icon), label, Modifier.size(fig(40)), colorFilter = ColorFilter.tint(tint))
                            } else {
                                MyTabIcon(tint, Modifier.size(fig(40)).semantics { contentDescription = label })
                            }
                        }
                    }
                }
            }
        }
        DriveAlertHost(vm, onClaimed = { id -> nav.navigate("analysis/$id") }, Modifier.align(Alignment.TopCenter).padding(top = D.ScreenPad))
    }
}
