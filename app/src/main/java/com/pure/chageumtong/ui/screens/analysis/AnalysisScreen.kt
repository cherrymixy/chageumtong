package com.pure.chageumtong.ui.screens.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.pure.chageumtong.domain.DriveSession
import com.pure.chageumtong.domain.praise
import com.pure.chageumtong.ui.AppViewModel
import com.pure.chageumtong.ui.components.BigButton
import com.pure.chageumtong.ui.components.GrayBox
import com.pure.chageumtong.ui.components.Label
import com.pure.chageumtong.ui.components.RouteMap
import com.pure.chageumtong.ui.components.SectionCard
import com.pure.chageumtong.ui.components.SemiGauge
import com.pure.chageumtong.ui.components.fmtKm
import com.pure.chageumtong.ui.theme.BODY
import com.pure.chageumtong.ui.theme.Blue
import com.pure.chageumtong.ui.theme.MIN_TOUCH
import com.pure.chageumtong.ui.theme.Orange
import com.pure.chageumtong.ui.theme.fig
import com.pure.chageumtong.ui.theme.figSp
import java.time.Instant

/** 주행 분석 결과 (PRD §6.2). readonly = 적립 내역에서 연 지난 주행. */
@Composable
fun AnalysisScreen(vm: AppViewModel, sessionId: String, readonly: Boolean, onDone: () -> Unit) {
    val data by vm.data.collectAsState()
    val session = data.sessions.find { it.id == sessionId }
    if (session == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Label("주행 기록을 찾을 수 없어요")
                BigButton("확인하기", onDone)
            }
        }
        return
    }
    var selected by remember(sessionId) { mutableIntStateOf(0) }

    Row(Modifier.fillMaxSize().padding(fig(24)), horizontalArrangement = Arrangement.spacedBy(fig(24))) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(fig(16))) {
            Header(session, readonly, vm)
            SectionCard(Modifier.fillMaxWidth()) {
                Label("이번 주행 분석")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(fig(24))) {
                    SemiGauge(session.safeRatio, Modifier.width(fig(280)))
                    Text(praise(session.safeRatio), fontSize = figSp(32), fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("이동거리", "${fmtKm(session.distanceKm)}km")
                    Stat("안전운전 거리", "${fmtKm(session.safeKm)}km")
                    Stat("주행시간", "${session.durationSec / 60}분")
                }
            }
            SectionCard(Modifier.fillMaxWidth().weight(1f)) {
                if (session.violations.isEmpty()) {
                    Text("모든 구간 적립 완료!", fontSize = figSp(32), fontWeight = FontWeight.Bold, color = Blue)
                    Label("위반 없이 달려서 모든 거리가 포인트로 적립됐어요.")
                } else {
                    Text("포인트 미적립 구간", fontSize = figSp(28), fontWeight = FontWeight.Bold)
                    Label("아래 구간에서 위반이 감지되어 앞뒤 250m의 포인트가 적립되지 않았어요.")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(fig(12))) {
                        itemsIndexed(session.violations) { i, v ->
                            FilterChip(
                                selected = i == selected,
                                onClick = { selected = i },
                                label = { Text(v.title, fontSize = BODY) },
                                modifier = Modifier.heightIn(min = MIN_TOUCH),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Orange,
                                    selectedLabelColor = Color.White,
                                ),
                            )
                        }
                    }
                }
            }
            BigButton("확인하기", onDone, Modifier.fillMaxWidth())
        }
        RouteMap(vm.demoRoute.polyline, session.distanceKm, session.violations, selected, Modifier.weight(1.1f).fillMaxHeight())
    }
}

@Composable
private fun Header(session: DriveSession, readonly: Boolean, vm: AppViewModel) {
    SectionCard(Modifier.fillMaxWidth(), color = Blue) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (readonly) {
                    val d = Instant.ofEpochMilli(session.startedAt).atZone(vm.zone)
                    Text("${d.monthValue}월 ${d.dayOfMonth}일 주행", fontSize = figSp(48), fontWeight = FontWeight.Bold, color = Color.White)
                    Text("+${session.points}P 적립", fontSize = BODY, color = Color.White)
                } else {
                    Text("+${session.points}P", fontSize = figSp(64), fontWeight = FontWeight.Bold, color = Color.White)
                    Text("적립이 완료됐어요!", fontSize = BODY, color = Color.White)
                }
            }
            GrayBox("당근", Modifier.size(fig(120)))
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Label(label)
        Text(value, fontSize = figSp(32), fontWeight = FontWeight.Bold)
    }
}
