package com.pure.chageumtong.ui.screens.analysis

import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.pure.chageumtong.domain.Violation
import com.pure.chageumtong.ui.RoutePath
import com.pure.chageumtong.ui.components.ViolationBubble
import com.pure.chageumtong.ui.theme.Alert
import com.pure.chageumtong.ui.theme.Primary
import com.pure.chageumtong.ui.theme.fig

/**
 * 분석 화면 지도 (카카오맵): 카카오내비 길찾기로 받은 실제 도로 경로(Primary) + 미적립 구간(Alert) + 선택 위반 말풍선.
 * 위반 위치는 km 비율로 경로에 투영 (실주행 GPS 기록은 범위 밖, PRD §11).
 * 말풍선은 고정 위치(ANCHOR)에 두고, 지도 패딩으로 선택 지점이 그 자리에 오도록 카메라를 옮긴다.
 * 그래서 지도 끌기·확대는 막아 둠 (움직이면 말풍선이 엉뚱한 곳을 가리킴).
 */
@Composable
fun KakaoRouteMap(path: List<List<Double>>, distanceKm: Double, violations: List<Violation>, selected: Int, modifier: Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var map by remember { mutableStateOf<KakaoMap?>(null) }
    val route = remember(path) { RoutePath(path) }
    val latLngs = remember(path) { path.map { LatLng.from(it[0], it[1]) } }
    fun f(km: Double) = if (distanceKm <= 0) 0.0 else km / distanceKm

    val mapView = remember {
        MapView(context).apply {
            start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() { map = null }
                    override fun onMapError(error: Exception) { Log.w("KakaoMap", "map error (키·키 해시 확인)", error) }
                },
                object : KakaoMapReadyCallback() {
                    override fun getPosition(): LatLng = route.at(0.5).let { LatLng.from(it[0], it[1]) }
                    override fun getZoomLevel(): Int = 13
                    override fun onMapReady(kakaoMap: KakaoMap) {
                        GestureType.values().forEach { runCatching { kakaoMap.setGestureEnable(it, false) } }
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

    // 경로선: 파란 전체 경로 위에 주황 미적립 구간을 덮음. 흰 테두리로 지도 위에서 잘 보이게 (카카오내비 경로 표시 방식)
    LaunchedEffect(map, violations, distanceKm) {
        val layer = map?.routeLineManager?.layer ?: return@LaunchedEffect
        layer.removeAll()
        // 카카오맵은 선 두께도 라벨처럼 xhdpi(2x) 기준으로 보고 줄여 그림 (160dpi에서 실측 절반) → 2x로 넘김
        val scale = 2f / density.density
        val w = with(density) { fig(18).toPx() } * scale
        val s = with(density) { fig(4).toPx() } * scale
        fun style(c: Color) = RouteLineStyles.from(RouteLineStyle.from(w, c.toArgb(), s, Color.White.toArgb()))
        layer.addRouteLine(RouteLineOptions.from(RouteLineSegment.from(latLngs, style(Primary))).setZOrder(0))
        violations.forEach { v ->
            val seg = route.slice(f(v.segmentStartKm), f(v.segmentEndKm)).map { LatLng.from(it[0], it[1]) }
            if (seg.size >= 2) layer.addRouteLine(RouteLineOptions.from(RouteLineSegment.from(seg, style(Alert))).setZOrder(1))
        }
    }

    BoxWithConstraints(modifier) {
        val wPx = with(density) { maxWidth.toPx() }.toInt()
        val hPx = with(density) { maxHeight.toPx() }.toInt()
        val ax = with(density) { ANCHOR_X.toPx() }.toInt()
        val ay = with(density) { ANCHOR_Y.toPx() }.toInt()
        val v = violations.getOrNull(selected)

        LaunchedEffect(map, v, wPx, hPx) {
            val m = map ?: return@LaunchedEffect
            val anim = CameraAnimation.from(500, true, true)
            if (v == null) {
                m.setPadding(0, 0, 0, 0)
                m.moveCamera(CameraUpdateFactory.fitMapPoints(latLngs.toTypedArray(), with(density) { fig(80).roundToPx() }), anim)
            } else {
                // 패딩을 준 나머지 영역의 가운데 = (ax, ay) → 선택 지점이 말풍선 꼬리 자리에 옴
                m.setPadding(0, 0, (wPx - 2 * ax).coerceAtLeast(0), (hPx - 2 * ay).coerceAtLeast(0))
                val p = route.at(f(v.atKm))
                m.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(p[0], p[1]), 15), anim)
            }
        }

        AndroidView({ mapView }, Modifier.fillMaxSize())
        v?.let { ViolationBubble(it, Modifier.offset(ANCHOR_X + fig(24), ANCHOR_Y + fig(24)).width(fig(588))) }
    }
}

// 말풍선(588×362)이 지도 영역(973×1023) 안에 들어가는 왼쪽 위 지점
private val ANCHOR_X = fig(140)
private val ANCHOR_Y = fig(200)
