package com.pure.chageumtong.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.pure.chageumtong.domain.DriveState
import com.pure.chageumtong.domain.praise
import com.pure.chageumtong.ui.AppViewModel
import com.pure.chageumtong.ui.components.BigButton
import com.pure.chageumtong.ui.components.GrayBox
import com.pure.chageumtong.ui.components.Label
import com.pure.chageumtong.ui.components.ProgressRail
import com.pure.chageumtong.ui.components.SectionCard
import com.pure.chageumtong.ui.components.SemiGauge
import com.pure.chageumtong.ui.components.WeekBars
import com.pure.chageumtong.ui.components.fmtKm
import com.pure.chageumtong.ui.components.fmtP
import com.pure.chageumtong.ui.theme.BODY
import com.pure.chageumtong.ui.theme.Blue
import com.pure.chageumtong.ui.theme.BoxGray
import com.pure.chageumtong.ui.theme.TextSub
import com.pure.chageumtong.ui.theme.fig
import com.pure.chageumtong.ui.theme.figSp
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun HomeScreen(vm: AppViewModel, onClaimed: (String) -> Unit, onShop: () -> Unit, onHistory: () -> Unit) {
    Row(Modifier.fillMaxSize().padding(fig(24)), horizontalArrangement = Arrangement.spacedBy(fig(24))) {
        DriveCard(vm, onClaimed, Modifier.weight(1f).fillMaxHeight())
        Column(Modifier.width(fig(560)).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(fig(16))) {
            TotalCard(vm, onShop, onHistory)
            Row(horizontalArrangement = Arrangement.spacedBy(fig(16))) {
                SafeCard(vm, Modifier.weight(1f))
                DistanceCard(vm, Modifier.weight(1f))
            }
            AdCarousel(vm, Modifier.fillMaxWidth().weight(1f))
        }
    }
}

/** 좌측 메인 카드: 이번 주행 */
@Composable
private fun DriveCard(vm: AppViewModel, onClaimed: (String) -> Unit, modifier: Modifier) {
    val live by vm.live.collectAsState()
    val driving by vm.isDriving.collectAsState()
    val demoRunning by vm.demoRunning.collectAsState()

    SectionCard(modifier) {
        Label("이번 주행으로 적립될 포인트")
        Text("${live.livePoints}P", fontSize = figSp(96), fontWeight = FontWeight.Bold)
        GrayBox(
            if (live.state == DriveState.DRIVING) "도시 일러스트 + 달리는 유니콘 🥕" else "도시 일러스트 + 유니콘 (정지)",
            Modifier.fillMaxWidth().weight(1f),
        )
        ProgressRail(live.distanceKm, live.violations, Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Stat("주행거리", "${fmtKm(live.distanceKm)}km")
            Stat("주행시간", "${live.durationSec / 60}분")
            Stat("안전점수", "${live.safeRatio}점")
            Spacer(Modifier.weight(1f))
            if (live.state == DriveState.IDLE && !demoRunning && !driving) {
                BigButton("▶ 데모 주행", vm::startDemo, container = BoxGray, content = Color.Black)
                Spacer(Modifier.width(fig(16)))
            }
            when {
                live.state == DriveState.ENDED && !live.canClaim -> {
                    Label("0.3km 미만 주행은 적립되지 않아요")
                    Spacer(Modifier.width(fig(16)))
                    BigButton("확인", vm::dismissDrive)
                }
                driving -> BigButton("정차 후 적립할 수 있어요", {}, enabled = false)
                else -> BigButton("주행 분석하고 적립하기", { vm.claim(onClaimed) }, enabled = live.canClaim)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(Modifier.padding(end = fig(32))) {
        Label(label)
        Text(value, fontSize = figSp(32), fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TotalCard(vm: AppViewModel, onShop: () -> Unit, onHistory: () -> Unit) {
    val data by vm.data.collectAsState()
    val month by vm.month.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }

    SectionCard(Modifier.fillMaxWidth(), color = Blue) {
        // 숨김 메뉴: 제목 길게 누르기 → 데이터 초기화 (PRD §8)
        Text(
            "총 적립 포인트",
            Modifier.combinedClickable(onClick = {}, onLongClick = { confirmReset = true }),
            fontSize = BODY,
            color = Color.White,
        )
        Text("${fmtP(data.totalPoints)}P", fontSize = figSp(56), fontWeight = FontWeight.Bold, color = Color.White)
        Text("이번 달 +${fmtP(month?.earned ?: 0)}P 적립", fontSize = BODY, color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(fig(12))) {
            BigButton("포인트 사용하기", onShop, Modifier.weight(1f), container = Color.White, content = Blue)
            BigButton("적립 내역 확인하기", onHistory, Modifier.weight(1f), container = Color.White, content = Blue)
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("데이터 초기화", fontSize = BODY) },
            text = { Text("주행·포인트 기록을 처음 상태(시드 데이터)로 되돌릴까요?", fontSize = BODY) },
            confirmButton = { TextButton({ vm.resetAll(); confirmReset = false }) { Text("초기화", fontSize = BODY) } },
            dismissButton = { TextButton({ confirmReset = false }) { Text("취소", fontSize = BODY) } },
        )
    }
}

@Composable
private fun SafeCard(vm: AppViewModel, modifier: Modifier) {
    val month by vm.month.collectAsState()
    val ratio = month?.safeRatio ?: 0
    SectionCard(modifier) {
        Label("이번 달 안전운전 거리")
        Text("${fmtKm(month?.safeKm ?: 0.0)}km", fontSize = figSp(36), fontWeight = FontWeight.Bold)
        SemiGauge(ratio, Modifier.fillMaxWidth())
        Text(
            praise(ratio),
            Modifier.align(Alignment.CenterHorizontally).background(BoxGray, RoundedCornerShape(fig(32))).padding(horizontal = fig(16), vertical = fig(4)),
            fontSize = BODY,
        )
    }
}

@Composable
private fun DistanceCard(vm: AppViewModel, modifier: Modifier) {
    val month by vm.month.collectAsState()
    val week by vm.week.collectAsState()
    val today = LocalDate.now(vm.zone).dayOfWeek.value - 1
    SectionCard(modifier) {
        Label("이번 달 주행거리")
        Text("${fmtKm(month?.distanceKm ?: 0.0)}km", fontSize = figSp(36), fontWeight = FontWeight.Bold)
        WeekBars(week, today, Modifier.fillMaxWidth().height(fig(190)))
    }
}

private const val AD_COUNT = 5

/** 광고 배너 캐러셀: 5초 자동 넘김, 주행 중 정지 (PRD §7) */
@Composable
private fun AdCarousel(vm: AppViewModel, modifier: Modifier) {
    val driving by vm.isDriving.collectAsState()
    val pager = rememberPagerState { AD_COUNT }
    LaunchedEffect(driving) {
        while (!driving) {
            delay(5_000)
            pager.animateScrollToPage((pager.currentPage + 1) % AD_COUNT)
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(fig(8))) {
        HorizontalPager(pager, Modifier.fillMaxWidth().weight(1f)) { page ->
            Box(Modifier.fillMaxSize()) {
                GrayBox("광고 배너 ${page + 1}", Modifier.fillMaxSize())
                Text(
                    "Ad",
                    Modifier.padding(fig(12)).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(fig(8))).padding(horizontal = fig(8)),
                    color = Color.White,
                    fontSize = BODY,
                )
            }
        }
        Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(fig(8))) {
            repeat(AD_COUNT) { i ->
                Box(Modifier.size(fig(12)).background(if (i == pager.currentPage) Blue else TextSub.copy(alpha = 0.4f), CircleShape))
            }
        }
    }
}
