package com.pure.chageumtong.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pure.chageumtong.domain.Product
import com.pure.chageumtong.domain.ShopTab
import com.pure.chageumtong.ui.AppViewModel
import com.pure.chageumtong.ui.components.AssetImage
import com.pure.chageumtong.ui.components.FigButton
import com.pure.chageumtong.ui.components.PointsCard
import com.pure.chageumtong.ui.components.fmtP
import com.pure.chageumtong.ui.theme.Alert
import com.pure.chageumtong.ui.theme.ChipOff
import com.pure.chageumtong.ui.theme.D
import com.pure.chageumtong.ui.theme.MIN_TOUCH
import com.pure.chageumtong.ui.theme.Primary
import com.pure.chageumtong.ui.theme.Scrim
import com.pure.chageumtong.ui.theme.Surface
import com.pure.chageumtong.ui.theme.T
import com.pure.chageumtong.ui.theme.TextChipOff
import com.pure.chageumtong.ui.theme.TextHint
import com.pure.chageumtong.ui.theme.TextTertiary
import com.pure.chageumtong.ui.theme.TextTitle
import com.pure.chageumtong.ui.theme.TextToggleOff
import com.pure.chageumtong.ui.theme.Bg
import com.pure.chageumtong.ui.theme.Track
import com.pure.chageumtong.ui.theme.fig

/** 상점 (Figma 95:2870, PRD §6.4). 가격 = 포인트 1:1. */
@Composable
fun ShopScreen(vm: AppViewModel) {
    val data by vm.data.collectAsState()
    val driving by vm.isDriving.collectAsState()
    var tab by remember { mutableStateOf(ShopTab.BLUE_LINK) }
    var confirm by remember { mutableStateOf<Product?>(null) }
    val total = data.totalPoints
    LaunchedEffect(driving) { if (driving) confirm = null } // 정차 중 열어둔 구매 창도 출발하면 닫음 (PRD §7)

    Column(Modifier.fillMaxSize().padding(start = fig(31), top = fig(32), end = fig(31))) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TabToggle(tab) { tab = it }
            Spacer(Modifier.weight(1f))
            if (driving) Text("주행 중에는 구매할 수 없어요", Modifier.padding(end = fig(24)), style = T.Section, color = Alert)
            PointsCard(total)
        }
        Spacer(Modifier.height(fig(37)))
        LazyVerticalGrid(
            GridCells.Fixed(3),
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = fig(24)),
            horizontalArrangement = Arrangement.spacedBy(fig(24)),
            verticalArrangement = Arrangement.spacedBy(fig(24)),
        ) {
            items(vm.products.filter { it.tab == tab }, key = { it.id }) { p ->
                val owned = p.id in data.ownedProductIds
                ProductCard(p, owned, enabled = !owned && !driving) { confirm = p } // 주행 중 구매 비활성 (PRD §7)
            }
        }
    }

    confirm?.let { p ->
        PurchaseDialog(p, total, onDismiss = { confirm = null }) {
            vm.purchase(p) {}
            confirm = null
        }
    }
}

/** SHOP / BLUE LINK 토글 (95:2917~2920): 회색 알약 436×103 안에 선택 탭만 파란 알약 */
@Composable
private fun TabToggle(tab: ShopTab, onSelect: (ShopTab) -> Unit) {
    val pill = RoundedCornerShape(fig(51.5))
    Row(Modifier.size(fig(436), fig(103)).clip(pill).background(ChipOff).padding(fig(8))) {
        listOf(ShopTab.SHOP to "SHOP", ShopTab.BLUE_LINK to "BLUE LINK").forEach { (t, label) ->
            val on = t == tab
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(pill).background(if (on) Primary else Color.Transparent)
                    .clickable { onSelect(t) },
                contentAlignment = Alignment.Center,
            ) { Text(label, style = if (on) T.ToggleOn else T.ToggleOff, color = if (on) Bg else TextToggleOff) }
        }
    }
}

/** 상품 카드 (95:2879): 사진 + 위아래 어두운 그라데이션, 이름·가격·흰 버튼 */
@Composable
private fun ProductCard(p: Product, owned: Boolean, enabled: Boolean, onBuy: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(fig(368)).clip(RoundedCornerShape(fig(32)))) {
        AssetImage(p.imageAsset, Modifier.fillMaxSize())
        Box(Modifier.fillMaxWidth().height(fig(151)).background(Brush.verticalGradient(listOf(Scrim, Scrim.copy(alpha = 0f)))))
        Box(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().height(fig(151))
                .background(Brush.verticalGradient(listOf(Scrim.copy(alpha = 0f), Scrim))),
        )
        Text(p.name, Modifier.offset(fig(32), fig(32)).padding(end = fig(64)), style = T.ProductName, color = Color.White, maxLines = 1)
        Text(
            p.priceLabel,
            Modifier.align(Alignment.BottomStart).padding(start = fig(36), bottom = fig(34)),
            style = T.ProductPrice.copy(shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(2f, 2f), 4f)),
            color = Color.White,
        )
        // 보이는 버튼은 Figma 175×57, 누르는 영역은 최소 76dp (PRD §7)
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = fig(8), bottom = fig(10))
                .size(fig(195), MIN_TOUCH).clickable(enabled = enabled, onClick = onBuy),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(fig(175), fig(57)).clip(RoundedCornerShape(fig(20))).background(if (enabled) Color.White else Track),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (owned) "보유중" else "포인트 사용하기", style = T.SmallButton, color = if (enabled) Primary else TextHint)
            }
        }
    }
}

/** 구매 확인 팝업 (PRD §6.6, Figma 없음 → 상점 카드·분석 버튼 스타일로 설계). 부족하면 "포인트가 nP 부족해요" + 확인만. */
@Composable
private fun PurchaseDialog(p: Product, total: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val short = p.price - total
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.width(fig(720)).clip(RoundedCornerShape(fig(32))).background(Surface).padding(fig(36)),
            verticalArrangement = Arrangement.spacedBy(fig(28)),
        ) {
            if (short > 0) Text("포인트가 ${fmtP(short)}P 부족해요", style = T.DialogTitle, color = Alert)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(fig(28))) {
                AssetImage(p.imageAsset, Modifier.size(fig(240), fig(167)).clip(RoundedCornerShape(fig(20))))
                Column(verticalArrangement = Arrangement.spacedBy(fig(8))) {
                    Text(p.name, style = T.DialogName, color = TextTitle)
                    Text("${fmtP(p.price)}P 사용", style = T.DialogPrice, color = Primary)
                    Text(
                        if (short > 0) "보유 ${fmtP(total)}P" else "사용 후 잔액 ${fmtP(total - p.price)}P",
                        style = T.DialogBody, color = TextTertiary,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(fig(16))) {
                val h = Modifier.weight(1f).height(D.BottomBarHeight)
                if (short > 0) {
                    FigButton("확인", onDismiss, h, style = T.ButtonLarge)
                } else {
                    FigButton("취소", onDismiss, h, container = ChipOff, content = TextChipOff, style = T.ButtonLarge)
                    FigButton("사용하기", onConfirm, h, style = T.ButtonLarge)
                }
            }
        }
    }
}
