package com.pure.chageumtong.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/** 입력원(실차/데모)이 내보내는 한 순간의 차량 상태. timestampMs는 샘플 시각(데모는 압축된 가상 시각). */
data class VehicleSample(val timestampMs: Long, val speedMps: Double, val gear: Gear)

/** 지금 달리는 도로 구간 정보. 실주행은 기본값(제한 60, 직선). */
data class RoadSegment(
    val limitKmh: Int = DEFAULT_LIMIT_KMH,
    val isCurve: Boolean = false,
    val locationLabel: String = "",
)

const val DEFAULT_LIMIT_KMH = 60

data class LiveDrive(
    val state: DriveState = DriveState.IDLE,
    val startedAtMs: Long? = null,
    val endedAtMs: Long? = null,
    val distanceKm: Double = 0.0,
    val durationSec: Long = 0,
    val violations: List<Violation> = emptyList(),
) {
    val livePoints: Int get() = points(distanceKm, violations)
    val safeRatio: Int get() = safeRatio(distanceKm, violations)
    val canClaim: Boolean get() = state == DriveState.ENDED && canClaim(distanceKm)
}

/**
 * 샘플 스트림 → 주행 상태머신·거리 적분·위반 판정 (PRD §2, §3).
 * 입력원이 실차인지 데모인지 모른다. 시간은 오직 샘플 시각으로 계산한다.
 */
class DriveSessionEngine {
    private val _live = MutableStateFlow(LiveDrive())
    val live: StateFlow<LiveDrive> = _live.asStateFlow()

    private var last: VehicleSample? = null
    private var lastSegment: RoadSegment? = null
    private var overSinceMs: Long? = null
    private var overCounted = false
    private var currentLabel = ""

    fun onSample(sample: VehicleSample, segment: RoadSegment = RoadSegment()) {
        val cur = _live.value
        when (cur.state) {
            // ENDED에서 다시 출발하면 새 주행. 이전 세션 저장은 상위 계층(STEP 4) 책임.
            DriveState.IDLE, DriveState.ENDED -> if (sample.gear == Gear.D && sample.speedMps > STOP_MPS) {
                resetTracking()
                _live.value = LiveDrive(state = DriveState.DRIVING, startedAtMs = sample.timestampMs)
                last = sample
                checkRules(sample, segment, 0.0)
            }
            DriveState.DRIVING -> {
                val prev = last ?: sample
                val dtSec = ((sample.timestampMs - prev.timestampMs).coerceAtLeast(0)) / 1000.0
                val distanceKm = cur.distanceKm + (prev.speedMps + sample.speedMps) / 2 * dtSec / 1000.0 // 사다리꼴
                val ended = sample.speedMps <= STOP_MPS && sample.gear == Gear.P
                _live.value = cur.copy(
                    state = if (ended) DriveState.ENDED else DriveState.DRIVING,
                    distanceKm = distanceKm,
                    durationSec = (sample.timestampMs - (cur.startedAtMs ?: sample.timestampMs)) / 1000,
                    endedAtMs = if (ended) sample.timestampMs else null,
                )
                last = sample
                if (!ended) checkRules(sample, segment, distanceKm)
            }
        }
    }

    /** 외부 이벤트 주입 (안전거리 미확보: ADAS 미지원이라 데모만). 주행 중일 때만 기록. */
    fun injectViolation(type: ViolationType, locationLabel: String = currentLabel) {
        val cur = _live.value
        if (cur.state != DriveState.DRIVING) return
        addViolation(Violation.at(type, cur.distanceKm, locationLabel))
    }

    /** 적립 완료 후 대기 상태로. */
    fun reset() {
        resetTracking()
        _live.value = LiveDrive()
    }

    private fun checkRules(sample: VehicleSample, segment: RoadSegment, atKm: Double) {
        val kmh = sample.speedMps * 3.6
        currentLabel = segment.locationLabel

        // 과속: 제한+10 "초과"가 3초 지속되면 1건. 계속 과속 중이면 추가 X. (EPS: 60/3.6*3.6 같은 부동소수 오차)
        val overKmh = kmh - segment.limitKmh
        if (overKmh > OVERSPEED_MARGIN_KMH + EPS_KMH) {
            val since = overSinceMs ?: sample.timestampMs.also { overSinceMs = it }
            if (!overCounted && sample.timestampMs - since >= OVERSPEED_HOLD_MS) {
                overCounted = true
                addViolation(Violation.at(ViolationType.OVERSPEED, atKm, segment.locationLabel, overKmh.roundToInt()))
            }
        } else {
            overSinceMs = null
            overCounted = false
        }

        // 급커브: 커브 구간에 "진입하는 순간" 속도가 40km/h 초과면 1건
        if (segment.isCurve && segment != lastSegment && kmh > CURVE_MAX_KMH + EPS_KMH) {
            addViolation(Violation.at(ViolationType.SHARP_CURVE, atKm, segment.locationLabel))
        }
        lastSegment = segment
    }

    private fun addViolation(v: Violation) {
        _live.value = _live.value.let { it.copy(violations = it.violations + v) }
    }

    private fun resetTracking() {
        last = null
        lastSegment = null
        overSinceMs = null
        overCounted = false
        currentLabel = ""
    }

    companion object {
        const val STOP_MPS = 0.1
        const val OVERSPEED_MARGIN_KMH = 10.0
        const val OVERSPEED_HOLD_MS = 3_000L
        const val CURVE_MAX_KMH = 40.0
        private const val EPS_KMH = 0.01
    }
}
