package com.pure.chageumtong.domain

import kotlinx.serialization.Serializable

enum class Gear { P, R, N, D, UNKNOWN }

/** ENDED = 정차(P) 후 분석·적립 대기 */
enum class DriveState { IDLE, DRIVING, ENDED }

enum class ViolationType { OVERSPEED, SHARP_CURVE, SAFE_DISTANCE }

@Serializable
data class Violation(
    val type: ViolationType,
    val atKm: Double,
    val segmentStartKm: Double,
    val segmentEndKm: Double,
    val locationLabel: String,
    val overKmh: Int? = null,
) {
    /** 미적립 칩 문구 (PRD §3) */
    val title: String
        get() = when (type) {
            ViolationType.OVERSPEED -> "${overKmh ?: 0}km 과속"
            ViolationType.SHARP_CURVE -> "급커브 구간 미감속"
            ViolationType.SAFE_DISTANCE -> "안전거리 미확보"
        }

    val description: String
        get() = when (type) {
            ViolationType.OVERSPEED ->
                "제한속도보다 ${overKmh ?: 0}km/h 빠르게 주행한 것이 감지되어 해당 구간의 포인트가 적립되지 않았어요. 제한속도를 지켜 안전하게 주행해 주세요."
            ViolationType.SHARP_CURVE ->
                "급커브 구간에서 충분한 감속 없이 주행한 것이 감지되어 해당 구간의 포인트가 적립되지 않았어요. 커브 진입 전 미리 속도를 줄여 안전하게 주행해 주세요."
            ViolationType.SAFE_DISTANCE ->
                "앞차와의 안전거리가 확보되지 않은 것이 감지되어 해당 구간의 포인트가 적립되지 않았어요. 충분한 거리를 두고 주행해 주세요."
        }

    companion object {
        /** 위반 지점 기준 ±250m (PRD §12 D1) */
        fun at(type: ViolationType, atKm: Double, locationLabel: String, overKmh: Int? = null) = Violation(
            type = type,
            atKm = atKm,
            segmentStartKm = (atKm - VIOLATION_HALF_KM).coerceAtLeast(0.0),
            segmentEndKm = atKm + VIOLATION_HALF_KM,
            locationLabel = locationLabel,
            overKmh = overKmh,
        )
    }
}

@Serializable
data class DriveSession(
    val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val distanceKm: Double,
    val durationSec: Long,
    val violations: List<Violation>,
    val routeId: String?, // 데모 경로 연결용
    val claimed: Boolean,
) {
    val unsafeKm: Double get() = unsafeKm(violations, distanceKm)
    val safeKm: Double get() = safeKm(distanceKm, violations)
    val safeRatio: Int get() = safeRatio(distanceKm, violations)
    val points: Int get() = points(distanceKm, violations)
}

/** amount: +적립 / −사용 */
@Serializable
data class PointTx(
    val id: String,
    val at: Long,
    val amount: Int,
    val sessionId: String?,
    val productId: String?,
    val label: String,
)

enum class ShopTab { SHOP, BLUE_LINK }

@Serializable
data class Product(
    val id: String,
    val tab: ShopTab,
    val name: String,
    val priceLabel: String,
    val price: Int,
    val imageAsset: String,
)

enum class PlaceCategory { CHARGER, DEALER, BLUEHANDS }

/** mapX/mapY: 정적 지도 이미지 위 정규화 좌표 0~1 */
@Serializable
data class Place(
    val id: String,
    val category: PlaceCategory,
    val name: String,
    val subtitle: String,
    val minutes: Int,
    val meters: Int,
    val open: Boolean,
    val mapX: Float,
    val mapY: Float,
    /** 길안내 목적지 (시연용 근사 좌표, WGS84) */
    val lat: Double,
    val lon: Double,
    val imageAsset: String,
)
