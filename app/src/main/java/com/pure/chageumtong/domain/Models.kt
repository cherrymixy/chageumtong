package com.pure.chageumtong.domain

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

enum class Gear { P, R, N, D, UNKNOWN }

/** ENDED = 정차(P) 후 분석·적립 대기 */
enum class DriveState { IDLE, DRIVING, ENDED }

enum class ViolationType { OVERSPEED, SHARP_CURVE, SAFE_DISTANCE, HARD_START, HARD_ACCEL, HARD_DECEL, HARD_STOP }

/**
 * 미적립 판정 1건. 새 필드는 모두 기본값이 있어 예전에 저장된 세션 JSON도 그대로 읽힌다.
 * grade: 위험도 등급 1~4 / speedKmh: 정지시거 계산에 쓴 속도 / waived: 유예 건(차감·표시 안 함) / voidsTrip: 4등급(그 주행 전체 미적립)
 */
@Serializable
data class Violation(
    val type: ViolationType,
    val atKm: Double,
    val segmentStartKm: Double,
    val segmentEndKm: Double,
    val locationLabel: String,
    val overKmh: Int? = null,
    val grade: Int = 1,
    val speedKmh: Int? = null,
    val waived: Boolean = false,
    val voidsTrip: Boolean = false,
) {
    /** 미적립 칩 문구 (PRD §3) */
    val title: String
        get() = when (type) {
            ViolationType.OVERSPEED -> "${overKmh ?: 0}km 과속"
            ViolationType.SHARP_CURVE -> "급커브 구간 미감속"
            ViolationType.SAFE_DISTANCE -> "안전거리 미확보"
            ViolationType.HARD_START -> "급출발"
            ViolationType.HARD_ACCEL -> "급가속"
            ViolationType.HARD_DECEL -> "급감속"
            ViolationType.HARD_STOP -> "급정지"
        }

    val description: String
        get() = when (type) {
            ViolationType.OVERSPEED ->
                "제한속도보다 ${overKmh ?: 0}km/h 빠르게 주행한 것이 감지되어 해당 구간의 포인트가 적립되지 않았어요. 제한속도를 지켜 안전하게 주행해 주세요."
            ViolationType.SHARP_CURVE ->
                "급커브 구간에서 충분한 감속 없이 주행한 것이 감지되어 해당 구간의 포인트가 적립되지 않았어요. 커브 진입 전 미리 속도를 줄여 안전하게 주행해 주세요."
            ViolationType.SAFE_DISTANCE ->
                "앞차와의 안전거리가 확보되지 않은 것이 감지되어 해당 구간의 포인트가 적립되지 않았어요. 충분한 거리를 두고 주행해 주세요."
            ViolationType.HARD_START ->
                "출발할 때 속도를 갑자기 올린 것이 감지되어 해당 구간의 포인트가 적립되지 않았어요. 부드럽게 출발해 주세요."
            ViolationType.HARD_ACCEL ->
                "속도를 갑자기 올린 것이 감지되어 해당 구간의 포인트가 적립되지 않았어요. 천천히 가속해 주세요."
            ViolationType.HARD_DECEL ->
                "속도를 갑자기 줄인 것이 감지되어 해당 구간의 포인트가 적립되지 않았어요. 앞차와 거리를 넉넉히 두고 미리 줄여 주세요."
            ViolationType.HARD_STOP ->
                "급하게 멈춘 것이 감지되어 해당 구간의 포인트가 적립되지 않았어요. 앞 상황을 미리 살피고 여유 있게 멈춰 주세요."
        }

    companion object {
        /** 순간 행동(1등급): 지점 앞뒤로 정지시거×1의 절반씩 */
        fun momentary(type: ViolationType, atKm: Double, speedKmh: Double, locationLabel: String, waived: Boolean = false): Violation {
            val halfKm = stoppingSightM(speedKmh) * coefForGrade(1) / 1000.0 / 2
            return Violation(
                type = type,
                atKm = atKm,
                segmentStartKm = (atKm - halfKm).coerceAtLeast(0.0),
                segmentEndKm = atKm + halfKm,
                locationLabel = locationLabel,
                speedKmh = speedKmh.roundToInt(),
                waived = waived,
            )
        }

        /** 과속 에피소드: 과속 구간 + (2·3등급만) 앞뒤로 정지시거×계수의 절반씩. 4등급은 주행 전체 미적립. */
        fun overspeed(startKm: Double, endKm: Double, maxOverKmh: Double, maxSpeedKmh: Double, locationLabel: String): Violation {
            val grade = gradeForOverKmh(maxOverKmh)
            val bufKm = if (grade in 2..3) stoppingSightM(maxSpeedKmh) * coefForGrade(grade) / 1000.0 / 2 else 0.0
            return Violation(
                type = ViolationType.OVERSPEED,
                atKm = startKm,
                segmentStartKm = (startKm - bufKm).coerceAtLeast(0.0),
                segmentEndKm = endKm + bufKm,
                locationLabel = locationLabel,
                overKmh = maxOverKmh.roundToInt(),
                grade = grade,
                speedKmh = maxSpeedKmh.roundToInt(),
                voidsTrip = grade == 4,
            )
        }
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
    /** 화면에 보여줄 위반 (유예 건 제외) */
    val visibleViolations: List<Violation> get() = violations.filterNot { it.waived }
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

enum class PlaceCategory { CHARGER, DEALER, BLUEHANDS, GAS }

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
    /** 인터넷 사진 (카카오 이미지 검색 결과). 있으면 imageAsset 위에 덮어 보여줌 */
    val imageUrl: String? = null,
)
