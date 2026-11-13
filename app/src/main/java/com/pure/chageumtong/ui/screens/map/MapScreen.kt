package com.pure.chageumtong.ui.screens.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import androidx.compose.ui.res.painterResource
import com.pure.chageumtong.BuildConfig
import kotlin.math.roundToInt
import com.pure.chageumtong.R
import com.pure.chageumtong.domain.Place
import com.pure.chageumtong.domain.PlaceCategory
import com.pure.chageumtong.ui.AppViewModel
import com.pure.chageumtong.ui.components.AssetImage
import com.pure.chageumtong.ui.theme.Alert
import com.pure.chageumtong.ui.theme.Bg
import com.pure.chageumtong.ui.theme.D
import com.pure.chageumtong.ui.theme.DealerRed
import com.pure.chageumtong.ui.theme.GasGreen
import com.pure.chageumtong.ui.theme.MIN_TOUCH
import com.pure.chageumtong.ui.theme.Primary
import com.pure.chageumtong.ui.theme.PrimaryTint
import com.pure.chageumtong.ui.theme.T
import com.pure.chageumtong.ui.theme.TextPlace
import com.pure.chageumtong.ui.theme.TextPlaceSub
import com.pure.chageumtong.ui.theme.TextSearch
import com.pure.chageumtong.ui.theme.TextTertiary
import com.pure.chageumtong.ui.theme.Track
import com.pure.chageumtong.ui.theme.fig

private val PlaceCategory.label get() = when (this) {
    PlaceCategory.CHARGER -> "충전소"
    PlaceCategory.DEALER -> "대리점"
    PlaceCategory.BLUEHANDS -> "블루핸즈"
    PlaceCategory.GAS -> "주유소"
}

/** Figma 95:2827/2829/2831 칩 글자색 */
private val PlaceCategory.color get() = when (this) {
    PlaceCategory.CHARGER -> Primary
    PlaceCategory.DEALER -> DealerRed
    PlaceCategory.BLUEHANDS -> Alert
    PlaceCategory.GAS -> GasGreen
}

private fun distance(m: Int) = if (m < 1000) "${m}m" else "%.1fkm".format(m / 1000.0)

/** 지도 — 포인트 사용처 (Figma 95:2751, PRD §6.3). 카카오 키가 있으면 실제 카카오맵, 없으면 정적 지도 그림. */
@Composable
fun MapScreen(vm: AppViewModel) {
    val driving by vm.isDriving.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val places by vm.places.collectAsState()
    var query by remember { mutableStateOf("") }
    var filters by remember { mutableStateOf(emptySet<PlaceCategory>()) }
    val context = LocalContext.current
    // 주행 중엔 검색어를 고칠 수 없으니, 출발하면 비워서 목록 전체를 보여줌 (PRD §7)
    LaunchedEffect(driving) { if (driving) query = "" }

    val navigate: (Place) -> Unit = { p ->
        vm.navigate(p) { ok ->
            val msg = if (ok) "${p.name}까지 길안내를 시작해요" else "내비게이션으로 안내를 시작할 수 없어요"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val list = places.filter { p ->
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
            ) {
                itemsIndexed(list, key = { _, p -> p.id }) { i, p ->
                    Column {
                        // 카드 사이 디바이더 (Figma엔 없음, 사진 없는 카드가 섞여 구분용). 위아래 여백 합 = 기존 카드 간격 57
                        if (i > 0) HorizontalDivider(Modifier.padding(vertical = fig(28)), thickness = fig(1.5), color = Track)
                        PlaceCard(
                            p,
                            favorite = p.id in favorites,
                            onFavorite = { vm.toggleFavorite(p.id) },
                            onNavigate = { navigate(p) },
                        )
                    }
                }
            }
        }
        PlaceMap(list, filters, vm.mapCenter, navigate, Modifier.weight(1f).fillMaxHeight()) { c ->
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
        // 사진: 검색 사진 → 시드 그림 순. 둘 다 없거나 불러오기 실패면 사진 영역 자체를 뺌
        val photo = Modifier.fillMaxWidth().height(fig(219)).clip(RoundedCornerShape(fig(21.3)))
        var urlFailed by remember(p.imageUrl) { mutableStateOf(false) }
        val url = p.imageUrl?.takeUnless { urlFailed }
        if (url != null) {
            AsyncImage(url, null, photo, contentScale = ContentScale.Crop, onError = { urlFailed = true })
        } else if (p.imageAsset.isNotEmpty()) {
            AssetImage(p.imageAsset, photo)
        }
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

/** 우측 지도 (95:2752): 지도(카카오맵 또는 일러스트) + 장소 핀 + 유니콘 현재 위치 + 내 위치 버튼 + 필터 칩 */
@Composable
private fun PlaceMap(
    places: List<Place>,
    filters: Set<PlaceCategory>,
    center: Pair<Double, Double>,
    onPick: (Place) -> Unit,
    modifier: Modifier,
    onFilter: (PlaceCategory) -> Unit,
) {
    Box(modifier) {
        if (BuildConfig.KAKAO_NATIVE_KEY.isNotBlank()) KakaoMapLayer(places, center, onPick) else StaticLayer(places)
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

/** 카카오 키가 없을 때: 지도 일러스트 + 정규화 좌표 핀 + 유니콘 (기존 정적 지도) */
@Composable
private fun StaticLayer(places: List<Place>) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.map_static), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
    }
}

/**
 * 실제 카카오맵 (Map SDK v2). 장소 = 분류색 원형 라벨, 현재 위치 = 유니콘 라벨, 라벨을 누르면 길안내.
 * SDK 요구: start()로 시작, 화면 resume/pause를 그대로 전달, 떠날 때 finish().
 */
@Composable
private fun KakaoMapLayer(places: List<Place>, center: Pair<Double, Double>, onPick: (Place) -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var map by remember { mutableStateOf<KakaoMap?>(null) }
    val pick by rememberUpdatedState(onPick)
    val here = LatLng.from(center.first, center.second)
    // 카카오맵은 라벨 비트맵을 xhdpi(2x) 기준으로 보고 화면 밀도에 맞춰 줄인다(160dpi에선 0.5배) → 2x 픽셀로 넘김
    val labelScale = 2f / density.density
    val pinPx = with(density) { (fig(36).toPx() * labelScale).roundToInt() }
    val px = { v: Float -> with(density) { fig(v).toPx() * labelScale } }
    val (unicorn, unicornAnchorY) = remember(density) { unicornMarker(context, px(128f), px(11f), px(29f), px(4f)) }

    val mapView = remember {
        MapView(context).apply {
            start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() { map = null }
                    override fun onMapError(error: Exception) {
                        Log.w("KakaoMap", "map error (키·키 해시 확인)", error)
                    }
                },
                object : KakaoMapReadyCallback() {
                    override fun getPosition(): LatLng = here
                    override fun getZoomLevel(): Int = 14
                    override fun onMapReady(kakaoMap: KakaoMap) {
                        kakaoMap.setOnLabelClickListener { _, _, label -> (label.tag as? Place)?.let(pick); true }
                        map = kakaoMap
                    }
                },
            )
        }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) mapView.resume()
            if (e == Lifecycle.Event.ON_PAUSE) mapView.pause()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); mapView.finish() }
    }
    // 목록·필터가 바뀌면 라벨을 새로 그림
    LaunchedEffect(map, places) {
        val m = map ?: return@LaunchedEffect
        val layer = m.labelManager?.layer ?: return@LaunchedEffect
        layer.removeAll()
        // 유니콘은 장소 핀보다 위에 (rank 높을수록 위), 아래 파란 점이 실제 위치
        layer.addLabel(
            LabelOptions.from(here).setRank(1).setStyles(LabelStyle.from(unicorn).setAnchorPoint(0.5f, unicornAnchorY)),
        )
        places.forEach { p ->
            layer.addLabel(
                LabelOptions.from(LatLng.from(p.lat, p.lon))
                    .setStyles(LabelStyle.from(pinBitmap(pinPx, p.category.color)))
                    .setClickable(true)
                    .setTag(p),
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView({ mapView }, Modifier.fillMaxSize())
        // 내 위치 버튼 (95:2868): 지도 중심을 현재 위치로
        Image(
            painterResource(R.drawable.ic_crosshair), "내 위치로",
            Modifier.align(Alignment.BottomStart).padding(start = fig(43 - 19), bottom = fig(123 - 24)).size(fig(102))
                .clickable { map?.moveCamera(CameraUpdateFactory.newCenterPosition(here), CameraAnimation.from(500, true, true)) },
        )
        // 확대·축소 (Figma에 없음, 지도 오른쪽 아래). 버튼 하나 76×76, 흰 카드는 필터 칩과 같은 모양
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = fig(43), bottom = fig(32))
                .shadow(fig(8), RoundedCornerShape(D.RadiusButton)).background(Color.White, RoundedCornerShape(D.RadiusButton)),
        ) {
            ZoomButton("+", "지도 확대") { map?.moveCamera(CameraUpdateFactory.zoomIn(), CameraAnimation.from(300, true, true)) }
            HorizontalDivider(Modifier.width(MIN_TOUCH), thickness = fig(1.5), color = Track)
            ZoomButton("−", "지도 축소") { map?.moveCamera(CameraUpdateFactory.zoomOut(), CameraAnimation.from(300, true, true)) }
        }
    }
}

@Composable
private fun ZoomButton(symbol: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(MIN_TOUCH).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(symbol, style = T.Button, color = TextPlace) }
}

/**
 * 현재 위치 마커 = 유니콘 + 간격 + 파란 점(흰 테두리), 정적 지도(95:2848)와 같은 모양.
 * 반환: 비트맵과 앵커 y(0~1, 파란 점 중심) — 점이 실제 좌표에 오도록.
 */
private fun unicornMarker(context: android.content.Context, size: Float, gap: Float, dot: Float, ring: Float): Pair<Bitmap, Float> {
    val w = size.roundToInt()
    val h = (size + gap + dot).roundToInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val src = BitmapFactory.decodeResource(context.resources, R.drawable.unicorn_marker)
    canvas.drawBitmap(Bitmap.createScaledBitmap(src, w, w, true), 0f, 0f, null)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cy = size + gap + dot / 2
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(w / 2f, cy, dot / 2, paint)
    paint.color = Primary.toArgb()
    canvas.drawCircle(w / 2f, cy, dot / 2 - ring, paint)
    return bmp to cy / h
}

/** 흰 테두리 + 분류색 원 (정적 지도 핀과 같은 모양: 36 중 테두리 6) */
private fun pinBitmap(size: Int, color: Color): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val r = size / 2f
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(r, r, r, paint)
    paint.color = color.toArgb()
    canvas.drawCircle(r, r, r * (12f / 18f), paint)
    return bmp
}
