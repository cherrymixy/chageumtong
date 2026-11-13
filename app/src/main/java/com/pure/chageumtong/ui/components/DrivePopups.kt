package com.pure.chageumtong.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import com.pure.chageumtong.R
import com.pure.chageumtong.domain.Gear
import com.pure.chageumtong.ui.AppViewModel
import com.pure.chageumtong.ui.theme.ChipOff
import com.pure.chageumtong.ui.theme.Handle
import com.pure.chageumtong.ui.theme.PopupSub
import com.pure.chageumtong.ui.theme.PopupTitle
import com.pure.chageumtong.ui.theme.Primary
import com.pure.chageumtong.ui.theme.Surface
import com.pure.chageumtong.ui.theme.T
import com.pure.chageumtong.ui.theme.fig
import kotlinx.coroutines.delay

private const val AD_POINTS = 5
private const val AD_SECONDS = 5

/** Figma 프레임(97:2250)이 0.5배로 그려져 있어 2배로 변환 (버튼 38 → 76 = MIN_TOUCH) */
private fun f2(px: Number) = fig(px.toFloat() * 2)

/**
 * 앱 안 상단 팝업 3종 (Figma 97:2250). 우선순위: 재탑승 > 주행 끝 > N모드 광고.
 * - 주행 끝: 적립 가능한 주행이 끝나고 P로 2초 지나면
 * - 재탑승: 적립 안 한 주행이 있는데 정차 상태로 P에서 다른 기어로 바꾸면 (출발하면 새 주행이 시작돼 이전 주행은 적립 못 함)
 * - N모드: N + 정차로 5초 지나면 광고 보고 5P. 움직이거나 기어를 바꾸면 바로 닫힘 (PRD §7)
 */
@Composable
fun DrivePopupHost(vm: AppViewModel, onClaimed: (String) -> Unit, modifier: Modifier = Modifier) {
    val live by vm.live.collectAsState()
    val input by vm.input.collectAsState()
    val gear = input?.sample?.gear
    val stopped = (input?.sample?.speedMps ?: 0.0) <= 0.1
    val key = live.startedAtMs
    var dismissed by remember { mutableStateOf(setOf<String>()) }

    val endCond = live.canClaim && gear == Gear.P
    var endReady by remember { mutableStateOf(false) }
    LaunchedEffect(endCond, key) {
        endReady = false
        if (endCond) { delay(2000); endReady = true }
    }
    val reCond = live.canClaim && stopped && gear != null && gear != Gear.P && gear != Gear.UNKNOWN
    val nCond = gear == Gear.N && stopped && !live.canClaim
    var nReady by remember { mutableStateOf(false) }
    var nDismissed by remember(nCond) { mutableStateOf(false) } // N으로 다시 서면 또 띄움
    var ad by remember { mutableStateOf(false) }
    LaunchedEffect(nCond) {
        nReady = false
        if (nCond) { delay(5000); nReady = true } else ad = false
    }

    fun claim(tag: String) {
        dismissed = dismissed + tag
        vm.claim(onClaimed)
    }

    when {
        reCond && "re-$key" !in dismissed -> Popup(
            "잊어버린 포인트가 있어요..", "출발하기 전, 잊지말고 포인트를 적립하세요!", "포인트 적립하기",
            onAction = { claim("re-$key") }, onClose = { dismissed = dismissed + "re-$key" }, modifier,
        )
        endCond && endReady && "end-$key" !in dismissed -> Popup(
            "🚗 ${live.livePoints}P가 쌓였어요!", "빠르게 주행 분석하고 포인트를 적립하세요!", "포인트 적립하기",
            onAction = { claim("end-$key") }, onClose = { dismissed = dismissed + "end-$key" }, modifier,
        )
        nCond && nReady && !nDismissed && !ad -> Popup(
            "잠시 정차 중 ${AD_POINTS}P 받기!", "광고 보고 추가 적립하세요!", "광고 보고 ${AD_POINTS}P받기",
            onAction = { ad = true }, onClose = { nDismissed = true }, modifier,
        )
    }
    if (ad) {
        AdDialog(onDone = { vm.addAdBonus(AD_POINTS); ad = false; nDismissed = true }, onClose = { ad = false; nDismissed = true })
    }
}

/** 팝업 (Figma 105:2251, 392×157 → ×2) */
@Composable
private fun Popup(title: String, sub: String, action: String, onAction: () -> Unit, onClose: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(f2(28))
    Column(
        modifier.width(f2(392)).shadow(fig(12), shape).clip(shape).background(Color.White)
            .padding(start = f2(14), top = f2(13.6), end = f2(16), bottom = f2(14)),
    ) {
        Row {
            Image(
                painterResource(R.drawable.app_logo), null,
                Modifier.size(f2(69.13)).clip(RoundedCornerShape(f2(16.04))), contentScale = ContentScale.Crop,
            )
            Column(Modifier.padding(start = f2(13), top = f2(4.4))) {
                Text(title, style = T.PopupTitle, color = PopupTitle, maxLines = 1)
                Spacer(Modifier.height(f2(7.8)))
                Text(sub, style = T.PopupSub, color = PopupSub, maxLines = 1)
            }
        }
        Spacer(Modifier.height(f2(22.3)))
        Row(horizontalArrangement = Arrangement.spacedBy(f2(16))) {
            PopupButton(action, Primary, Color.White, onAction)
            PopupButton("닫기", ChipOff, PopupTitle, onClose)
        }
    }
}

@Composable
private fun PopupButton(text: String, bg: Color, fg: Color, onClick: () -> Unit) = Box(
    Modifier.size(f2(173), f2(38)).clip(RoundedCornerShape(f2(14))).background(bg).clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
) { Text(text, style = T.PopupButton, color = fg, maxLines = 1) }

/** 광고 시청: 기존 광고 배너를 5초 보여주고 끝나면 적립 (실제 광고 SDK 없음) */
@Composable
private fun AdDialog(onDone: () -> Unit, onClose: () -> Unit) {
    var left by remember { mutableIntStateOf(AD_SECONDS) }
    LaunchedEffect(Unit) {
        while (left > 0) { delay(1000); left-- }
        onDone()
    }
    Dialog(onDismissRequest = onClose) {
        Box(Modifier.clip(RoundedCornerShape(fig(24))).background(Surface)) {
            Image(painterResource(R.drawable.ad_banner_1), "광고", Modifier.size(fig(1224 * 0.7), fig(558 * 0.7)), contentScale = ContentScale.Crop)
            Text(
                if (left > 0) "${left}초 후 ${AD_POINTS}P 적립" else "${AD_POINTS}P 적립 완료",
                Modifier.align(Alignment.TopEnd).padding(fig(20)).clip(RoundedCornerShape(fig(19)))
                    .background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = fig(20), vertical = fig(10)),
                style = T.PopupButton, color = Color.White,
            )
        }
    }
}

/** 주행 중 위젯 (Figma 105:2337, 392×88 → ×2). 다른 앱 위 오버레이에서 쓴다 */
@Composable
fun DriveWidget(points: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(f2(392), f2(88)).clip(RoundedCornerShape(f2(16))).background(Color.White)) {
        Box(Modifier.align(Alignment.TopCenter).offset(y = f2(7)).size(f2(50), f2(3)).clip(RoundedCornerShape(f2(5))).background(Handle))
        Text("이번 주행 포인트", Modifier.offset(f2(20), f2(22)), style = T.WidgetLabel, color = PopupTitle)
        Box(Modifier.offset(f2(138), f2(18)).size(f2(242), f2(58)).clip(RoundedCornerShape(f2(9.78))).background(Primary)) {
            Row(Modifier.offset(f2(13), f2(4)), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(f2(4.4))) {
                Text("+$points", style = T.WidgetNumber, color = Color.White)
                Text("P", style = T.WidgetNumber, color = Color.White.copy(alpha = 0.5f))
            }
            Image(
                painterResource(R.drawable.unicorn_run), null,
                Modifier.offset(f2(166), f2(4.93)).size(f2(68), f2(48.38)),
            )
        }
    }
}
