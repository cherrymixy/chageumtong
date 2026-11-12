package com.pure.chageumtong.data.seed

import android.content.Context
import com.pure.chageumtong.domain.RoadSegment
import com.pure.chageumtong.domain.ViolationType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 데모 경로 (assets/demo_route.json). polyline: 분석 화면 지도용 정규화 좌표 [x, y] 0~1. */
@Serializable
data class DemoRoute(
    val id: String,
    val totalKm: Double,
    val segments: List<DemoSegment>,
    val events: List<DemoEvent>,
    val polyline: List<List<Float>>,
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
) {
    fun toRoadSegment() = RoadSegment(limitKmh, isCurve, label)
}

/** 센서로 감지할 수 없는 이벤트(안전거리 미확보)를 이 지점에서 주입 */
@Serializable
data class DemoEvent(val atKm: Double, val type: ViolationType, val label: String)

object DemoRouteLoader {
    fun load(context: Context): DemoRoute =
        Json.decodeFromString(context.assets.open("demo_route.json").bufferedReader().use { it.readText() })
}
