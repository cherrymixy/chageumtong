package com.pure.chageumtong.data.seed

import android.content.Context
import com.pure.chageumtong.domain.RoadSegment
import com.pure.chageumtong.domain.ViolationType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 데모 경로 (assets/demo_route.json).
 * polyline: 그림 지도(키 없을 때)용 정규화 좌표 [x, y] 0~1.
 * path: 카카오맵용 실제 도로 좌표 [위도, 경도] (scripts/fetch_kakao_route.py 가 카카오내비 길찾기로 채움).
 */
@Serializable
data class DemoRoute(
    val id: String,
    val totalKm: Double,
    val segments: List<DemoSegment>,
    val events: List<DemoEvent>,
    val polyline: List<List<Float>>,
    val path: List<List<Double>> = emptyList(),
    /** 도착 후 정지할 때 초당 감속(km/h). 7.5 이상이면 급정지로 판정된다 */
    val stopKmhPerS: Double = DEFAULT_RAMP_KMH_PER_S,
) {
    fun segmentAt(km: Double): DemoSegment = segments.lastOrNull { km >= it.startKm } ?: segments.first()
}

@Serializable
data class DemoSegment(
    val startKm: Double,
    val endKm: Double,
    val limitKmh: Int,
    val speedKmh: Double, // 데모 차량이 이 구간에서 달리는 속도
    val isCurve: Boolean = false,
    val label: String,
    /** 이 구간 속도로 바꿀 때 초당 변화량(km/h). 기본 4는 급가속(11)·급감속(7.5) 기준 아래 */
    val rampKmhPerS: Double = DEFAULT_RAMP_KMH_PER_S,
) {
    fun toRoadSegment() = RoadSegment(limitKmh, isCurve, label)
}

/** 센서로 감지할 수 없는 이벤트(안전거리 미확보)를 이 지점에서 주입 */
@Serializable
data class DemoEvent(val atKm: Double, val type: ViolationType, val label: String)

const val DEFAULT_RAMP_KMH_PER_S = 4.0

object DemoRouteLoader {
    fun load(context: Context): DemoRoute =
        Json.decodeFromString(context.assets.open("demo_route.json").bufferedReader().use { it.readText() })
}
