package com.pure.chageumtong.ui.screens.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.pure.chageumtong.R
import com.pure.chageumtong.domain.DriveSession
import com.pure.chageumtong.domain.PointTx
import com.pure.chageumtong.ui.AppViewModel
import com.pure.chageumtong.ui.components.PointsCard
import com.pure.chageumtong.ui.components.fmtKm
import com.pure.chageumtong.ui.components.fmtP
import com.pure.chageumtong.ui.theme.Alert
import com.pure.chageumtong.ui.theme.ChipOff
import com.pure.chageumtong.ui.theme.D
import com.pure.chageumtong.ui.theme.MIN_TOUCH
import com.pure.chageumtong.ui.theme.Primary
import com.pure.chageumtong.ui.theme.StrokeGray
import com.pure.chageumtong.ui.theme.Surface
import com.pure.chageumtong.ui.theme.T
import com.pure.chageumtong.ui.theme.TextChipOff
import com.pure.chageumtong.ui.theme.TextTertiary
import com.pure.chageumtong.ui.theme.TextTitle
import com.pure.chageumtong.ui.theme.fig
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** 적립 내역 한 줄: 주행(세션) 또는 세션 없는 거래(사용·이전 적립) */
private sealed interface Item {
    val at: Long
    data class Drive(val s: DriveSession) : Item { override val at get() = s.startedAt }
    data class Tx(val tx: PointTx) : Item { override val at get() = tx.at }
}

/** 적립 내역 (PRD §6.5, Figma 없음 → 분석 화면 뒤로 버튼·칩, 상점 포인트 카드, 홈 카드 스타일 재사용) */
@Composable
fun HistoryScreen(vm: AppViewModel, onOpenSession: (String) -> Unit, onBack: () -> Unit) {
    val data by vm.data.collectAsState()
    val zone = vm.zone
    val items = (data.sessions.map { Item.Drive(it) } + data.txs.filter { it.sessionId == null }.map { Item.Tx(it) })
        .sortedByDescending { it.at }
    fun ym(t: Long) = YearMonth.from(Instant.ofEpochMilli(t).atZone(zone))
    val months = items.map { ym(it.at) }.distinct()
    var month by remember { mutableStateOf<YearMonth?>(null) }
    val current = month ?: months.firstOrNull()
    val fmt = remember(zone) { DateTimeFormatter.ofPattern("M월 d일 HH:mm").withZone(zone) }

    Column(Modifier.fillMaxSize().padding(start = D.ScreenPad, top = D.ScreenPad, end = D.ScreenPad)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(fig(84), D.BottomBarHeight).clip(RoundedCornerShape(D.RadiusCard)).background(Surface).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Image(painterResource(R.drawable.ic_caret_left), "뒤로", Modifier.size(fig(40))) }
            Text("적립 내역", Modifier.padding(start = fig(24)).weight(1f), style = T.DialogTitle, color = TextTitle)
            PointsCard(data.totalPoints)
        }
        Spacer(Modifier.height(fig(28)))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(fig(16))) {
            items(months) { m ->
                val on = m == current
                Box(
                    Modifier.height(MIN_TOUCH).clip(RoundedCornerShape(D.RadiusButton))
                        .background(if (on) Primary else ChipOff).clickable { month = m }.padding(horizontal = fig(28)),
                    contentAlignment = Alignment.Center,
                ) { Text("${m.year}년 ${m.monthValue}월", style = T.Chip, color = if (on) Color.White else TextChipOff) }
            }
        }
        Spacer(Modifier.height(fig(24)))
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = fig(24)),
            verticalArrangement = Arrangement.spacedBy(fig(12)),
        ) {
            items(items.filter { ym(it.at) == current }) { item ->
                when (item) {
                    is Item.Drive -> HistoryRow(
                        "${fmt.format(Instant.ofEpochMilli(item.s.startedAt))} 주행",
                        "${fmtKm(item.s.distanceKm)}km · 안전 ${item.s.safeRatio}%",
                        "+${fmtP(item.s.points)}P",
                        Primary,
                        Modifier.clickable { onOpenSession(item.s.id) },
                    )
                    is Item.Tx -> HistoryRow(
                        fmt.format(Instant.ofEpochMilli(item.tx.at)),
                        item.tx.label,
                        (if (item.tx.amount < 0) "−" else "+") + "${fmtP(kotlin.math.abs(item.tx.amount))}P",
                        if (item.tx.amount < 0) Alert else Primary,
                    )
                }
            }
        }
    }
}

/** 한 줄 카드: 홈 카드와 같은 흰 바탕·1px 테두리·라운드 24 */
@Composable
private fun HistoryRow(title: String, sub: String, amount: String, amountColor: Color, modifier: Modifier = Modifier) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(D.RadiusCard)).then(modifier)
            .background(Surface).border(D.StrokeCard, StrokeGray, RoundedCornerShape(D.RadiusCard))
            .heightIn(min = fig(112)).padding(horizontal = fig(36), vertical = fig(16)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(fig(6))) {
            Text(title, style = T.RowTitle, color = TextTitle)
            Text(sub, style = T.DialogBody, color = TextTertiary)
        }
        Text(amount, style = T.RowAmount, color = amountColor)
    }
}
