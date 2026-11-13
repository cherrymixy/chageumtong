package com.pure.chageumtong.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
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
import com.pure.chageumtong.domain.DriveSession
import com.pure.chageumtong.domain.PointTx
import com.pure.chageumtong.ui.AppViewModel
import com.pure.chageumtong.ui.components.BigButton
import com.pure.chageumtong.ui.components.Label
import com.pure.chageumtong.ui.components.SectionCard
import com.pure.chageumtong.ui.components.fmtKm
import com.pure.chageumtong.ui.components.fmtP
import com.pure.chageumtong.ui.theme.BODY
import com.pure.chageumtong.ui.theme.Blue
import com.pure.chageumtong.ui.theme.BoxGray
import com.pure.chageumtong.ui.theme.MIN_TOUCH
import com.pure.chageumtong.ui.theme.Orange
import com.pure.chageumtong.ui.theme.fig
import com.pure.chageumtong.ui.theme.figSp
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** 적립 내역 한 줄: 주행(세션) 또는 세션 없는 거래(사용·이전 적립) */
private sealed interface Item {
    val at: Long
    data class Drive(val s: DriveSession) : Item { override val at get() = s.startedAt }
    data class Tx(val tx: PointTx) : Item { override val at get() = tx.at }
}

/** 적립 내역 (PRD §6.5) */
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

    Column(Modifier.fillMaxSize().padding(fig(24)), verticalArrangement = Arrangement.spacedBy(fig(16))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(fig(24))) {
            BigButton("← 홈", onBack, container = BoxGray, content = Color.Black)
            Text("적립 내역", fontSize = figSp(40), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            SectionCard(color = Blue) {
                Text("총 적립 포인트 ${fmtP(data.totalPoints)}P", fontSize = BODY, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(fig(12))) {
            items(months) { m ->
                FilterChip(
                    selected = m == current,
                    onClick = { month = m },
                    label = { Text("${m.year}년 ${m.monthValue}월", fontSize = BODY) },
                    modifier = Modifier.heightIn(min = MIN_TOUCH),
                )
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(fig(8))) {
            items(items.filter { ym(it.at) == current }) { item ->
                when (item) {
                    is Item.Drive -> HistoryRow(
                        "${fmt.format(Instant.ofEpochMilli(item.s.startedAt))} 주행",
                        "${fmtKm(item.s.distanceKm)}km · 안전 ${item.s.safeRatio}%",
                        "+${fmtP(item.s.points)}P",
                        Blue,
                        Modifier.clickable { onOpenSession(item.s.id) },
                    )
                    is Item.Tx -> HistoryRow(
                        fmt.format(Instant.ofEpochMilli(item.tx.at)),
                        item.tx.label,
                        (if (item.tx.amount < 0) "−" else "+") + "${fmtP(kotlin.math.abs(item.tx.amount))}P",
                        if (item.tx.amount < 0) Orange else Blue,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(title: String, sub: String, amount: String, amountColor: Color, modifier: Modifier = Modifier) {
    SectionCard(modifier.fillMaxWidth().heightIn(min = MIN_TOUCH)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = BODY, fontWeight = FontWeight.Bold)
                Label(sub)
            }
            Text(amount, fontSize = figSp(32), fontWeight = FontWeight.Bold, color = amountColor)
        }
    }
}
