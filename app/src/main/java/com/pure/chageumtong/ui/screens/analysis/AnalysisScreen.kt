package com.pure.chageumtong.ui.screens.analysis

import kotlin.math.floor
import com.pure.chageumtong.ui.theme.LocalCompact
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.pure.chageumtong.BuildConfig
import com.pure.chageumtong.R
import com.pure.chageumtong.domain.DriveSession
import com.pure.chageumtong.domain.praise
import com.pure.chageumtong.ui.AppViewModel
import com.pure.chageumtong.ui.components.FigButton
import com.pure.chageumtong.ui.components.countUp
import com.pure.chageumtong.ui.components.RouteMap
import com.pure.chageumtong.ui.components.SemiGauge
import com.pure.chageumtong.ui.components.fmtKm
import com.pure.chageumtong.ui.theme.Alert
import com.pure.chageumtong.ui.theme.ChipOff
import com.pure.chageumtong.ui.theme.D
import com.pure.chageumtong.ui.theme.Primary
import com.pure.chageumtong.ui.theme.PrimaryTint
import com.pure.chageumtong.ui.theme.Surface
import com.pure.chageumtong.ui.theme.T
import com.pure.chageumtong.ui.theme.TextChipOff
import com.pure.chageumtong.ui.theme.TextHint
import com.pure.chageumtong.ui.theme.TextSection
import com.pure.chageumtong.ui.theme.TextStat
import com.pure.chageumtong.ui.theme.TextTitle
import com.pure.chageumtong.ui.theme.Track
import com.pure.chageumtong.ui.theme.fig
import java.time.Instant
import kotlinx.coroutines.delay

/** 주행 분석 결과 (Figma 95:2638 / 95:2689). readonly = 적립 내역에서 연 지난 주행. */
@Composable
fun AnalysisScreen(vm: AppViewModel, sessionId: String, readonly: Boolean, onDone: () -> Unit) {
    val data by vm.data.collectAsState()
    val session = data.sessions.find { it.id == sessionId }
    var selected by remember(sessionId) { mutableIntStateOf(0) }
    // 새로 적립한 주행만 음성 안내 1회 (지난 기록 열람은 조용히)
    LaunchedEffect(sessionId, session != null) {
        if (!readonly && session != null) vm.announcePoints(session.points)
    }

    val compact = LocalCompact.current
    Column(Modifier.fillMaxSize().padding(start = D.ScreenPad, top = D.ScreenPad, end = D.ScreenPad)) {
        if (compact && session != null) {
            // 좁은 폭: 분석 패널 위, 지도 아래로 쌓고 세로 스크롤 (Figma 없음, 107:2 홈 규칙을 따름)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).clip(RoundedCornerShape(D.RadiusCard)).background(Surface),
            ) {
                LeftPanel(session, readonly, vm, selected, Modifier.fillMaxWidth().height(D.ContentHeight)) { selected = it }
                RouteMapFor(vm, session, selected, Modifier.fillMaxWidth().height(fig(700)))
            }
        } else Row(Modifier.fillMaxWidth().height(D.ContentHeight).clip(RoundedCornerShape(D.RadiusCard)).background(Surface)) {
            if (session == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("주행 기록을 찾을 수 없어요", style = T.Section, color = TextTitle)
                }
            } else {
                LeftPanel(session, readonly, vm, selected, Modifier.width(fig(667)).fillMaxHeight()) { selected = it }
                RouteMapFor(vm, session, selected, Modifier.weight(1f).fillMaxHeight())
            }
        }
        Spacer(Modifier.height(D.BottomGap))
        if (readonly) {
            // 지난 주행: 뒤로 (95:2746, 84×85)
            Box(
                Modifier.size(fig(84), D.BottomBarHeight).clip(RoundedCornerShape(D.RadiusCard)).background(Surface).clickable(onClick = onDone),
                contentAlignment = Alignment.Center,
            ) { Image(painterResource(R.drawable.ic_caret_left), "뒤로", Modifier.size(fig(40))) }
        } else {
            FigButton("확인하기", onDone, Modifier.size(fig(364), D.BottomBarHeight), style = T.ButtonLarge)
        }
    }
}

/** 카카오 키·경로 좌표가 있으면 실제 지도, 없으면 그림 지도 (심사용 빌드 등) */
@Composable
private fun RouteMapFor(vm: AppViewModel, session: DriveSession, selected: Int, mapMod: Modifier) {
    if (BuildConfig.KAKAO_NATIVE_KEY.isNotBlank() && vm.demoRoute.path.isNotEmpty()) {
        KakaoRouteMap(vm.demoRoute.path, session.distanceKm, session.visibleViolations, selected, mapMod)
    } else {
        RouteMap(vm.demoRoute.polyline, session.distanceKm, session.visibleViolations, selected, mapMod)
    }
}

@Composable
private fun LeftPanel(s: DriveSession, readonly: Boolean, vm: AppViewModel, selected: Int, modifier: Modifier, onSelect: (Int) -> Unit) {
    val compact = LocalCompact.current
    Box(modifier.background(Color.White)) {
        // 헤더 (높이 222, Figma 107:432): +NP 왼쪽, 체크 아이콘 + 문구 오른쪽
        Box(Modifier.fillMaxWidth().height(fig(222)).background(Primary).clip(RoundedCornerShape(0))) {
            // 적립 순간: 체크가 통 튀어 들어오고 +NP가 0부터 올라감. 지난 기록 열람은 정지 상태 (STEP 11)
            val pop = remember { Animatable(if (readonly) 1f else 0f) }
            LaunchedEffect(Unit) {
                if (readonly) return@LaunchedEffect
                delay(300) // 화면 전환 페이드가 끝난 뒤에 튀어야 보임
                pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessVeryLow))
            }
            Row(Modifier.offset(fig(36), fig(24)), verticalAlignment = Alignment.Bottom) {
                Text("+${countUp(s.points, start = if (readonly) s.points else 0, ms = 1200)}", style = T.Hero, color = Color.White)
                Text("P", style = T.Hero, color = Color.White)
            }
            // 아이콘 중심 x = 518.5 (486.5 + 64/2), 문구 시작 415 → 폭 207 칸에 가운데 정렬
            // 좁은 폭은 패널이 넓어져(786) 오른쪽 끝 기준으로 둠 (넓은 화면의 오른쪽 여백 45 유지)
            val checkPos = if (compact) Modifier.align(Alignment.TopEnd).offset(-fig(45), fig(65)) else Modifier.offset(fig(415), fig(65))
            Column(checkPos.width(fig(207)), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painterResource(R.drawable.ic_check_circle), null,
                    Modifier.size(fig(64)).graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                )
                Spacer(Modifier.height(fig(10)))
                val sub = if (readonly) {
                    val d = Instant.ofEpochMilli(s.startedAt).atZone(vm.zone)
                    "${d.monthValue}월 ${d.dayOfMonth}일 주행"
                } else {
                    "적립이 완료됐어요!"
                }
                Text(sub, style = T.HeaderSub, color = Color.White, maxLines = 1, softWrap = false)
            }
        }
        Text("이번 주행 분석", Modifier.offset(fig(36), fig(249)), style = T.Section, color = TextTitle)

        // 반원 게이지 (Group 14: 447 폭)
        val gaugePos = if (compact) Modifier.align(Alignment.TopCenter).offset(y = fig(293)) else Modifier.offset(fig(95), fig(293))
        Column(gaugePos.width(fig(447)), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomCenter) {
                SemiGauge(s.safeRatio, Modifier.size(fig(447), fig(251)), stroke = fig(55))
                Text("${s.safeRatio}%", style = T.GaugeLarge, color = Primary)
            }
            Text(
                praise(s.safeRatio),
                Modifier.background(PrimaryTint, CircleShape).padding(horizontal = fig(29), vertical = fig(19.5)),
                style = T.BadgeLarge, color = Primary,
            )
        }

        Row(Modifier.offset(fig(36), fig(636)).width(fig(600))) {
            Stat("이동거리", fmtKm(s.distanceKm), "km", Modifier.width(fig(207)))
            Stat("안전운전 거리", fmtKm(s.safeKm), "km", Modifier.width(fig(233)))
            Stat("주행시간", "${s.durationSec / 60}", "분", Modifier)
        }
        HorizontalDivider(Modifier.offset(y = fig(782)), thickness = fig(1.5), color = Track)

        if (s.visibleViolations.isEmpty()) {
            Text("모든 구간 적립 완료!", Modifier.offset(fig(30), fig(808)), style = T.SectionBold, color = Primary)
            Text("위반 없이 달려서 이동한 거리가 모두 포인트로 적립됐어요.", Modifier.offset(fig(30), fig(848)), style = T.Hint, color = TextHint)
        } else {
            Row(Modifier.offset(fig(30), fig(808)), verticalAlignment = Alignment.CenterVertically) {
                Text("포인트 미적립 구간", style = T.SectionBold, color = TextSection)
                // 위반 때문에 못 받은 포인트 = 이동거리 그대로였을 때 포인트 − 실제 포인트
                val lost = floor(s.distanceKm + 1e-9).toInt() - s.points
                if (lost > 0) {
                    Text(
                        "- ${lost}P",
                        Modifier.padding(start = fig(12)).background(Alert.copy(alpha = 0.15f), RoundedCornerShape(fig(10)))
                            .padding(horizontal = fig(12), vertical = fig(2)),
                        style = T.SectionBold, color = Alert,
                    )
                }
            }
            Text("구간별로 포인트가 인정되지 않은 이유를 확인해보세요.", Modifier.offset(fig(30), fig(848)), style = T.Hint, color = TextHint)
            LazyRow(
                Modifier.offset(y = fig(901)).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = fig(30)),
                horizontalArrangement = Arrangement.spacedBy(fig(28)),
            ) {
                itemsIndexed(s.visibleViolations) { i, v ->
                    val on = i == selected
                    Box(
                        Modifier.height(fig(92)).clip(RoundedCornerShape(D.RadiusButton))
                            .background(if (on) Alert else ChipOff).clickable { onSelect(i) }
                            .padding(horizontal = fig(24)),
                        contentAlignment = Alignment.Center,
                    ) { Text(v.title, style = T.Chip, color = if (on) Color.White else TextChipOff) }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, unit: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = T.Label, color = TextTitle)
        Spacer(Modifier.height(fig(20)))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(fig(2))) {
            Text(value, style = T.StatLarge, color = TextStat)
            Text(unit, style = T.StatUnit, color = TextStat, modifier = Modifier.padding(bottom = fig(6)))
        }
    }
}
