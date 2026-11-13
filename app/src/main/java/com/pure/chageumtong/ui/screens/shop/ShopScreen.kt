package com.pure.chageumtong.ui.screens.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.pure.chageumtong.domain.Product
import com.pure.chageumtong.domain.ShopTab
import com.pure.chageumtong.ui.AppViewModel
import com.pure.chageumtong.ui.components.BigButton
import com.pure.chageumtong.ui.components.GrayBox
import com.pure.chageumtong.ui.components.Label
import com.pure.chageumtong.ui.components.SectionCard
import com.pure.chageumtong.ui.components.fmtP
import com.pure.chageumtong.ui.theme.BODY
import com.pure.chageumtong.ui.theme.Blue
import com.pure.chageumtong.ui.theme.BoxGray
import com.pure.chageumtong.ui.theme.Orange
import com.pure.chageumtong.ui.theme.fig
import com.pure.chageumtong.ui.theme.figSp

/** 상점 (PRD §6.4). 가격 = 포인트 1:1. */
@Composable
fun ShopScreen(vm: AppViewModel) {
    val data by vm.data.collectAsState()
    val driving by vm.isDriving.collectAsState()
    var tab by remember { mutableStateOf(ShopTab.BLUE_LINK) }
    var confirm by remember { mutableStateOf<Product?>(null) }
    val total = data.totalPoints

    Column(Modifier.fillMaxSize().padding(fig(24)), verticalArrangement = Arrangement.spacedBy(fig(16))) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(fig(8))) {
                listOf(ShopTab.SHOP to "SHOP", ShopTab.BLUE_LINK to "BLUE LINK").forEach { (t, label) ->
                    BigButton(
                        label,
                        { tab = t },
                        container = if (t == tab) Blue else BoxGray,
                        content = if (t == tab) Color.White else Color.Black,
                    )
                }
            }
            if (driving) Label("주행 중에는 구매할 수 없어요", Modifier.padding(end = fig(24)), color = Orange)
            Column(horizontalAlignment = Alignment.End) {
                Label("사용 가능 포인트")
                Text("${fmtP(total)}P", fontSize = figSp(40), fontWeight = FontWeight.Bold)
            }
        }
        LazyVerticalGrid(
            GridCells.Fixed(3),
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(fig(16)),
            verticalArrangement = Arrangement.spacedBy(fig(16)),
        ) {
            items(vm.products.filter { it.tab == tab }, key = { it.id }) { p ->
                val owned = p.id in data.ownedProductIds
                SectionCard {
                    GrayBox("상품 이미지", Modifier.fillMaxWidth().aspectRatio(1.8f))
                    Text(p.name, fontSize = BODY, fontWeight = FontWeight.Bold, maxLines = 1)
                    Label(p.priceLabel)
                    BigButton(
                        if (owned) "보유중" else "포인트 사용하기",
                        { confirm = p },
                        Modifier.fillMaxWidth(),
                        enabled = !owned && !driving, // 주행 중 구매 비활성 (PRD §7)
                    )
                }
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

/** 구매 확인 팝업 (PRD §6.6). 부족하면 "포인트가 nP 부족해요" + 확인만. */
@Composable
private fun PurchaseDialog(p: Product, total: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val short = p.price - total
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (short > 0) "포인트가 ${fmtP(short)}P 부족해요" else p.name, fontSize = figSp(32), fontWeight = FontWeight.Bold)
        },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(fig(16)), verticalAlignment = Alignment.CenterVertically) {
                GrayBox("썸네일", Modifier.size(fig(120)))
                Column {
                    if (short > 0) Text(p.name, fontSize = BODY)
                    Text("${fmtP(p.price)}P 사용", fontSize = BODY, fontWeight = FontWeight.Bold, color = Blue)
                    Text(
                        if (short > 0) "보유 ${fmtP(total)}P" else "사용 후 잔액 ${fmtP(total - p.price)}P",
                        fontSize = BODY,
                    )
                }
            }
        },
        confirmButton = {
            if (short > 0) BigButton("확인", onDismiss) else BigButton("사용하기", onConfirm)
        },
        dismissButton = {
            if (short <= 0) BigButton("취소", onDismiss, container = BoxGray, content = Color.Black)
        },
    )
}
