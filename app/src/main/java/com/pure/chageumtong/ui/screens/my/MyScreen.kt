package com.pure.chageumtong.ui.screens.my

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.pure.chageumtong.BuildConfig
import com.pure.chageumtong.R
import com.pure.chageumtong.ui.AppViewModel
import com.pure.chageumtong.ui.components.PointsCard
import com.pure.chageumtong.ui.components.cardKm
import com.pure.chageumtong.ui.theme.Alert
import com.pure.chageumtong.ui.theme.D
import com.pure.chageumtong.ui.theme.MIN_TOUCH
import com.pure.chageumtong.ui.theme.Primary
import com.pure.chageumtong.ui.theme.StrokeGray
import com.pure.chageumtong.ui.theme.Surface
import com.pure.chageumtong.ui.theme.T
import com.pure.chageumtong.ui.theme.TextHint
import com.pure.chageumtong.ui.theme.TextNumber
import com.pure.chageumtong.ui.theme.TextTertiary
import com.pure.chageumtong.ui.theme.TextTitle
import com.pure.chageumtong.ui.theme.fig
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private enum class Popup { RULES, INFO, RESET }

/** 마이페이지 (Figma 없음 → 적립 내역·상점 카드 스타일 재사용). 로그인 없음: 이 기기에 저장된 기록만 보여준다 */
@Composable
fun MyScreen(vm: AppViewModel, onHistory: () -> Unit) {
    val data by vm.data.collectAsState()
    val driving by vm.isDriving.collectAsState()
    var popup by remember { mutableStateOf<Popup?>(null) }
    LaunchedEffect(driving) { if (driving && popup == Popup.RESET) popup = null } // 주행 중 초기화 금지 (PRD §7)

    val distance = data.sessions.sumOf { it.distanceKm }
    val safe = data.sessions.sumOf { it.safeKm }
    val score = if (distance <= 0) 0 else (safe / distance * 100).roundToInt()
    val days = data.sessions.minOfOrNull { it.startedAt }?.let {
        ChronoUnit.DAYS.between(Instant.ofEpochMilli(it).atZone(vm.zone).toLocalDate(), LocalDate.now(vm.zone)) + 1
    } ?: 1

    Column(Modifier.fillMaxSize().padding(start = D.ScreenPad, top = D.ScreenPad, end = D.ScreenPad)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("마이페이지", Modifier.weight(1f), style = T.DialogTitle, color = TextTitle)
            PointsCard(data.totalPoints)
        }
        Spacer(Modifier.height(fig(28)))
        Row(horizontalArrangement = Arrangement.spacedBy(D.ColumnGap)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(D.CardGap)) {
                Row(
                    Modifier.fillMaxWidth().card().padding(D.CardPad),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painterResource(R.drawable.unicorn_run), null, Modifier.size(fig(180)))
                    Spacer(Modifier.width(fig(32)))
                    Column(verticalArrangement = Arrangement.spacedBy(fig(10))) {
                        Text("차금통 드라이버", style = T.BubbleTitle, color = TextTitle)
                        Text("함께한 지 ${days}일 · 주행 ${data.sessions.size}회", style = T.DialogBody, color = TextTertiary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(D.CardGap)) {
                    Stat("누적 주행 거리", cardKm(distance), "km", Modifier.weight(1f))
                    Stat("안전운전 거리", cardKm(safe), "km", Modifier.weight(1f))
                    Stat("평균 안전점수", "$score", "점", Modifier.weight(1f), Primary)
                }
            }
            Column(Modifier.width(D.RightColumn), verticalArrangement = Arrangement.spacedBy(fig(12))) {
                MenuRow("적립 내역", onClick = onHistory)
                MenuRow("포인트 적립 규칙") { popup = Popup.RULES }
                MenuRow("앱 정보") { popup = Popup.INFO }
                Row(
                    Modifier.fillMaxWidth().card()
                        .toggleable(data.ttsOn, role = Role.Switch, onValueChange = vm::setTts)
                        .heightIn(min = fig(112)).padding(horizontal = fig(36)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("적립 음성 안내", style = T.RowTitle, color = TextTitle)
                        Text("적립이 끝나면 포인트를 소리로 알려줘요", style = T.DialogBody, color = TextTertiary)
                    }
                    Switch(data.ttsOn, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = Primary))
                }
                MenuRow(
                    if (driving) "데이터 초기화 (정차 후 가능)" else "데이터 초기화",
                    enabled = !driving,
                    color = Alert,
                ) { popup = Popup.RESET }
            }
        }
    }

    when (popup) {
        Popup.RULES -> InfoDialog(
            "포인트 적립 규칙",
            "· 안전운전 1km = 1P\n" +
                "· 과속·급커브 미감속·안전거리 미확보가 감지된 지점 앞뒤 250m는 적립되지 않아요\n" +
                "· 0.3km 미만 주행은 적립되지 않아요\n" +
                "· 정차(P) 후 [주행 분석하고 적립하기]를 눌러야 적립돼요\n" +
                "· 상점에서 상품 가격만큼 포인트가 차감돼요",
        ) { popup = null }
        Popup.INFO -> InfoDialog(
            "앱 정보",
            "차금통 ${BuildConfig.VERSION_NAME} · 팀 삼남매\n" +
                "지도·장소 정보 ⓒ Kakao\n" +
                "장소 사진: 이미지 검색 결과 (저작권은 원 게시자에게 있음)\n" +
                "글꼴 Pretendard (SIL OFL 1.1)\n" +
                "이미지 로딩 Coil (Apache 2.0)",
        ) { popup = null }
        Popup.RESET -> AlertDialog(
            onDismissRequest = { popup = null },
            title = { Text("데이터 초기화", style = T.Section) },
            text = { Text("주행·포인트 기록을 처음 상태(시드 데이터)로 되돌릴까요?", style = T.DialogBody) },
            confirmButton = { TextButton({ vm.resetAll(); popup = null }, Modifier.heightIn(min = MIN_TOUCH)) { Text("초기화", style = T.Button) } },
            dismissButton = { TextButton({ popup = null }, Modifier.heightIn(min = MIN_TOUCH)) { Text("취소", style = T.Button) } },
        )
        null -> Unit
    }
}

/** 홈 카드와 같은 흰 바탕·1px 테두리·라운드 24 */
private fun Modifier.card() = clip(RoundedCornerShape(D.RadiusCard)).background(Surface)
    .border(D.StrokeCard, StrokeGray, RoundedCornerShape(D.RadiusCard))

@Composable
private fun Stat(label: String, value: String, unit: String, modifier: Modifier, valueColor: Color = TextNumber) {
    Column(modifier.card().padding(D.CardPad)) {
        Text(label, style = T.Label, color = TextTitle)
        Spacer(Modifier.height(fig(13)))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(fig(4))) {
            Text(value, style = T.CardNumber, color = valueColor)
            Text(unit, style = T.StatUnit, color = TextHint)
        }
    }
}

@Composable
private fun MenuRow(title: String, enabled: Boolean = true, color: Color = TextTitle, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().card().clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = fig(112)).padding(horizontal = fig(36)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = T.RowTitle, color = if (enabled) color else TextHint)
        Image(painterResource(R.drawable.ic_caret_left), null, Modifier.size(fig(40)).rotate(180f))
    }
}

@Composable
private fun InfoDialog(title: String, body: String, onClose: () -> Unit) = AlertDialog(
    onDismissRequest = onClose,
    title = { Text(title, style = T.Section) },
    text = { Text(body, style = T.DialogBody) },
    confirmButton = { TextButton(onClose, Modifier.heightIn(min = MIN_TOUCH)) { Text("확인", style = T.Button) } },
)

/** shortcut: 마이 탭 아이콘 PNG가 없어 임시로 그린 사람 모양. 승아가 ic_tab_my.png를 주면 Image로 교체 */
@Composable
fun MyTabIcon(color: Color, modifier: Modifier) = Canvas(modifier) {
    val w = size.width
    drawCircle(color, radius = w * 0.2f, center = Offset(w / 2, w * 0.3f))
    drawArc(color, 180f, 180f, useCenter = true, topLeft = Offset(w * 0.15f, w * 0.6f), size = Size(w * 0.7f, w * 0.7f))
}
