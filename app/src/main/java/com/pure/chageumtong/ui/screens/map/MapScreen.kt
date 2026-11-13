package com.pure.chageumtong.ui.screens.map

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.pure.chageumtong.domain.Place
import com.pure.chageumtong.domain.PlaceCategory
import com.pure.chageumtong.ui.AppViewModel
import com.pure.chageumtong.ui.components.BigButton
import com.pure.chageumtong.ui.components.GrayBox
import com.pure.chageumtong.ui.components.Label
import com.pure.chageumtong.ui.components.SectionCard
import com.pure.chageumtong.ui.theme.BODY
import com.pure.chageumtong.ui.theme.Blue
import com.pure.chageumtong.ui.theme.BoxGray
import com.pure.chageumtong.ui.theme.MIN_TOUCH
import com.pure.chageumtong.ui.theme.Orange
import com.pure.chageumtong.ui.theme.TextSub
import com.pure.chageumtong.ui.theme.fig

private val PlaceCategory.label get() = when (this) {
    PlaceCategory.CHARGER -> "충전소"
    PlaceCategory.DEALER -> "대리점"
    PlaceCategory.BLUEHANDS -> "블루핸즈"
}

private val PlaceCategory.pinColor get() = when (this) {
    PlaceCategory.CHARGER -> Color(0xFF16A34A)
    PlaceCategory.DEALER -> Blue
    PlaceCategory.BLUEHANDS -> Orange
}

/** 지도 — 포인트 사용처 (PRD §6.3). 정적 지도 + 정규화 좌표 핀. */
@Composable
fun MapScreen(vm: AppViewModel) {
    val driving by vm.isDriving.collectAsState()
    val favorites by vm.favorites.collectAsState()
    var query by remember { mutableStateOf("") }
    var filters by remember { mutableStateOf(emptySet<PlaceCategory>()) }
    val context = LocalContext.current

    val list = vm.places.filter { p ->
        (filters.isEmpty() || p.category in filters) &&
            (query.isBlank() || p.name.contains(query.trim()) || p.subtitle.contains(query.trim()))
    }

    Row(Modifier.fillMaxSize().padding(fig(24)), horizontalArrangement = Arrangement.spacedBy(fig(24))) {
        Column(Modifier.width(fig(640)).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(fig(12))) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                enabled = !driving, // 주행 중 검색 입력 비활성 (PRD §7)
                singleLine = true,
                textStyle = TextStyle(fontSize = BODY),
                placeholder = { Text(if (driving) "정차 후 검색할 수 있어요" else "장소 이름으로 검색", fontSize = BODY) },
                modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(fig(12))) {
                PlaceCategory.entries.forEach { c ->
                    FilterChip(
                        selected = c in filters,
                        onClick = { filters = if (c in filters) filters - c else filters + c },
                        label = { Text(c.label, fontSize = BODY) },
                        modifier = Modifier.heightIn(min = MIN_TOUCH),
                    )
                }
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(fig(12))) {
                items(list, key = { it.id }) { p ->
                    PlaceCard(
                        p,
                        favorite = p.id in favorites,
                        onFavorite = { vm.toggleFavorite(p.id) },
                        // shortcut: STEP 9에서 NaviHelper.requestRoute로 교체
                        onNavigate = { Toast.makeText(context, "내비게이션으로 안내를 시작할 수 없어요", Toast.LENGTH_SHORT).show() },
                    )
                }
            }
        }
        StaticMap(list, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun PlaceCard(p: Place, favorite: Boolean, onFavorite: () -> Unit, onNavigate: () -> Unit) {
    SectionCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(fig(16)), verticalAlignment = Alignment.CenterVertically) {
            GrayBox("이미지", Modifier.size(fig(120)))
            Column(Modifier.weight(1f)) {
                Text(p.name, fontSize = BODY, fontWeight = FontWeight.Bold)
                Label("${p.category.label} · ${p.subtitle}")
                Label("차로 ${p.minutes}분 · ${"%.1f".format(p.meters / 1000.0)}km")
                Text(if (p.open) "영업중" else "영업 종료", fontSize = BODY, color = if (p.open) Blue else TextSub)
            }
            Box(
                Modifier.size(MIN_TOUCH).clickable(onClick = onFavorite),
                contentAlignment = Alignment.Center,
            ) { Text(if (favorite) "★" else "☆", fontSize = BODY, color = if (favorite) Orange else TextSub) }
        }
        BigButton("길안내 시작", onNavigate, Modifier.fillMaxWidth())
    }
}

@Composable
private fun StaticMap(places: List<Place>, modifier: Modifier) {
    BoxWithConstraints(modifier.background(BoxGray, RoundedCornerShape(fig(24)))) {
        Label("지도 이미지 자리", Modifier.align(Alignment.TopStart).padding(fig(16)))
        val pin = fig(48)
        places.forEach { p ->
            Box(
                Modifier.offset(maxWidth * p.mapX - pin / 2, maxHeight * p.mapY - pin / 2).size(pin)
                    .background(p.category.pinColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(p.category.label.take(1), fontSize = BODY, color = Color.White) }
        }
        // 현재 위치 = 유니콘 마커 (일러스트 들어오면 교체)
        val me = fig(64)
        Box(
            Modifier.offset(maxWidth * 0.5f - me / 2, maxHeight * 0.5f - me / 2).size(me).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("🦄", fontSize = BODY) }
    }
}
