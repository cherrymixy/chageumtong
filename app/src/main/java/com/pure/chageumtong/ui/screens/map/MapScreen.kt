package com.pure.chageumtong.ui.screens.map

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.pure.chageumtong.R
import com.pure.chageumtong.domain.Place
import com.pure.chageumtong.domain.PlaceCategory
import com.pure.chageumtong.ui.AppViewModel
import com.pure.chageumtong.ui.components.AssetImage
import com.pure.chageumtong.ui.theme.Alert
import com.pure.chageumtong.ui.theme.Bg
import com.pure.chageumtong.ui.theme.D
import com.pure.chageumtong.ui.theme.DealerRed
import com.pure.chageumtong.ui.theme.MIN_TOUCH
import com.pure.chageumtong.ui.theme.Primary
import com.pure.chageumtong.ui.theme.PrimaryTint
import com.pure.chageumtong.ui.theme.T
import com.pure.chageumtong.ui.theme.TextPlace
import com.pure.chageumtong.ui.theme.TextPlaceSub
import com.pure.chageumtong.ui.theme.TextSearch
import com.pure.chageumtong.ui.theme.TextTertiary
import com.pure.chageumtong.ui.theme.fig

private val PlaceCategory.label get() = when (this) {
    PlaceCategory.CHARGER -> "충전소"
    PlaceCategory.DEALER -> "대리점"
    PlaceCategory.BLUEHANDS -> "블루핸즈"
}

/** Figma 95:2827/2829/2831 칩 글자색 */
private val PlaceCategory.color get() = when (this) {
    PlaceCategory.CHARGER -> Primary
    PlaceCategory.DEALER -> DealerRed
    PlaceCategory.BLUEHANDS -> Alert
}

private fun distance(m: Int) = if (m < 1000) "${m}m" else "%.1fkm".format(m / 1000.0)

/** 지도 — 포인트 사용처 (Figma 95:2751, PRD §6.3). 정적 지도 + 정규화 좌표 핀. */
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

    Row(
        Modifier.fillMaxSize().padding(start = D.ScreenPad, top = D.ScreenPad, end = D.ScreenPad)
            .clip(RoundedCornerShape(D.RadiusCard)),
    ) {
        Column(Modifier.width(fig(557)).fillMaxHeight().background(Color.White)) {
            SearchBar(query, enabled = !driving) { query = it } // 주행 중 검색 입력 비활성 (PRD §7)
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = fig(36), top = fig(42), end = fig(36), bottom = fig(36)),
                verticalArrangement = Arrangement.spacedBy(fig(57)),
            ) {
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
        StaticMap(list, filters, Modifier.weight(1f).fillMaxHeight()) { c ->
            filters = if (c in filters) filters - c else filters + c
        }
    }
}

/** 검색바 (95:2754): 회색 알약 높이 69 + 돋보기 34 */
@Composable
private fun SearchBar(query: String, enabled: Boolean, onChange: (String) -> Unit) {
    Row(
        Modifier.padding(start = fig(36), top = fig(31), end = fig(36)).fillMaxWidth().height(MIN_TOUCH)
            .clip(RoundedCornerShape(fig(43))).background(Bg).padding(horizontal = fig(24)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(if (enabled) "포인트 사용처를 검색해보세요." else "정차 후 검색할 수 있어요", style = T.Search, color = TextSearch)
            }
            BasicTextField(query, onChange, Modifier.fillMaxWidth(), enabled = enabled, singleLine = true, textStyle = T.Search.copy(color = TextPlace))
        }
        Image(painterResource(R.drawable.ic_search), null, Modifier.size(fig(34)))
    }
}

/** 장소 카드 (95:2760): 사진 219 + 이름·분류·별 + 소요시간·거리·영업 + [길안내 시작] */
@Composable
private fun PlaceCard(p: Place, favorite: Boolean, onFavorite: () -> Unit, onNavigate: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(fig(20))) {
        AssetImage(p.imageAsset, Modifier.fillMaxWidth().height(fig(219)).clip(RoundedCornerShape(fig(21.3))))
        Box(Modifier.fillMaxWidth().padding(horizontal = fig(12))) {
            Column(verticalArrangement = Arrangement.spacedBy(fig(15))) {
                Column(verticalArrangement = Arrangement.spacedBy(fig(4))) {
                    Text(p.name, Modifier.padding(end = MIN_TOUCH), style = T.PlaceName, color = TextPlace)
                    Text(p.subtitle, style = T.PlaceSub, color = TextPlaceSub)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(fig(11))) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(fig(4))) {
                        Image(painterResource(R.drawable.ic_car), null, Modifier.size(fig(24)))
                        Text("${p.minutes}분", style = T.PlaceMeta, color = TextTertiary)
                    }
                    Dot()
                    Text(distance(p.meters), style = T.PlaceMeta, color = TextTertiary)
                    Dot()
                    Text(if (p.open) "영업중" else "영업 종료", style = T.PlaceMeta, color = if (p.open) TextTertiary else TextPlaceSub)
                }
            }
            // 별: 보이는 크기 32, 누르는 영역 76 (PRD §7)
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = fig(22), y = fig(-22)).size(MIN_TOUCH).clickable(onClick = onFavorite),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(R.drawable.ic_star), if (favorite) "즐겨찾기 해제" else "즐겨찾기",
                    Modifier.size(fig(32)),
                    colorFilter = if (favorite) ColorFilter.tint(Alert) else null,
                )
            }
            // [길안내 시작] 보이는 크기 138×54, 누르는 영역 76 (PRD §7)
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = fig(10), y = fig(11)).size(fig(158), MIN_TOUCH).clickable(onClick = onNavigate),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(fig(138), fig(54)).clip(RoundedCornerShape(D.RadiusButton)).background(PrimaryTint), contentAlignment = Alignment.Center) {
                    Text("길안내 시작", style = T.SmallButton, color = Primary)
                }
            }
        }
    }
}

@Composable
private fun Dot() = Box(Modifier.size(fig(2)).background(TextTertiary, CircleShape))

/** 우측 지도 (95:2752): 지도 일러스트 + 장소 핀 + 유니콘 현재 위치 + 내 위치 버튼 + 필터 칩 */
@Composable
private fun StaticMap(places: List<Place>, filters: Set<PlaceCategory>, modifier: Modifier, onFilter: (PlaceCategory) -> Unit) {
    BoxWithConstraints(modifier) {
        Image(painterResource(R.drawable.map_static), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        // shortcut: 핀은 분류색 원형 마커(지도 그림에 박힌 파란 핀과 별개), 실제 지도 SDK 붙일 때 교체
        val pin = fig(36)
        places.forEach { p ->
            Box(
                Modifier.offset(maxWidth * p.mapX - pin / 2, maxHeight * p.mapY - pin / 2).size(pin)
                    .shadow(fig(4), CircleShape).background(Color.White, CircleShape).padding(fig(6))
                    .background(p.category.color, CircleShape),
            )
        }
        // 현재 위치 = 유니콘 마커 (95:2848, 128 + 아래 파란 점 29). 위치는 Figma 비율 그대로
        Column(
            Modifier.offset(maxWidth * (550f / 1143f) - fig(64), maxHeight * (783f / 1184f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(fig(11)),
        ) {
            Image(painterResource(R.drawable.unicorn_marker), "현재 위치", Modifier.size(fig(128)))
            Box(Modifier.size(fig(29)).border(fig(4), Color.White, CircleShape).padding(fig(4)).background(Primary, CircleShape))
        }
        // 내 위치 버튼 (95:2868, 64 + 그림자 포함 102). 정적 지도라 동작 없음
        Image(
            painterResource(R.drawable.ic_crosshair), null,
            Modifier.align(Alignment.BottomStart).padding(start = fig(43 - 19), bottom = fig(123 - 24)).size(fig(102)),
        )
        // 필터 칩 (95:2825): 흰 칩 높이 64, 선택 시 분류색 10% (Figma에 선택 상태 없음 → 추가)
        Row(
            Modifier.align(Alignment.BottomStart).padding(start = fig(43), bottom = fig(32 - 6)),
            horizontalArrangement = Arrangement.spacedBy(fig(24)),
        ) {
            PlaceCategory.entries.forEach { c ->
                val on = c in filters
                Box(Modifier.height(MIN_TOUCH).clickable { onFilter(c) }, contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.height(fig(64)).shadow(fig(8), RoundedCornerShape(D.RadiusButton))
                            .background(if (on) c.color.copy(alpha = 0.15f).compositeOverWhite() else Color.White, RoundedCornerShape(D.RadiusButton))
                            .then(if (on) Modifier.border(fig(2), c.color, RoundedCornerShape(D.RadiusButton)) else Modifier)
                            .padding(horizontal = fig(24)),
                        contentAlignment = Alignment.Center,
                    ) { Text(c.label, style = T.Button, color = c.color) }
                }
            }
        }
    }
}

private fun Color.compositeOverWhite() = Color(
    red = red * alpha + (1 - alpha), green = green * alpha + (1 - alpha), blue = blue * alpha + (1 - alpha),
)
