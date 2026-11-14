package com.pure.chageumtong.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    /** 화면에 보여줄 위반 (유예 건 제외) */
    val visibleViolations: List<Violation> get() = violations.filterNot { it.waived }
}

/**
 * 샘플 스트림 → 주행 상태머신·거리 적분·위반 판정 (PRD §2, §3, 미적립 구간 산정 스펙).
 * 입력원이 실차인지 데모인지 모른다. 시간은 오직 샘플 시각으로 계산한다.
 */
class DriveSessionEngine {
    private val _live = MutableStateFlow(LiveDrive())
    val live: StateFlow<LiveDrive> = _live.asStateFlow()

    private var last: VehicleSample? = null
    private var lastSegment: RoadSegment? = null
    private var currentLabel = ""

    /** 1초 속도 변화량 계산용 최근 샘플 (정차 대기 중 샘플도 넣어 급출발을 잡는다) */
    private val history = ArrayDeque<VehicleSample>()
    private var lastAccelMs: Long? = null
    private var lastDecelMs: Long? = null
    private var decelIndex: Int? = null // 지금 묶음의 급감속 건 (급정지로 바꿀 때)
    private var decelSeen = false

    /** 진행 중인 과속 에피소드 */
    private class Episode(val startMs: Long, val startKm: Double, val label: String) {
        var confirmed = false
        var maxOverKmh = 0.0
        var maxSpeedKmh = 0.0
        var belowSinceMs: Long? = null
        var belowKm = 0.0
    }
    private var episode: Episode? = null

    fun onSample(sample: VehicleSample, segment: RoadSegment = RoadSegment()) {
        remember(sample)
        val cur = _live.value
        when (cur.state) {
            // ENDED에서 다시 출발하면 새 주행. 이전 세션 저장은 상위 계층(STEP 4) 책임.
            DriveState.IDLE, DriveState.ENDED -> if (sample.gear == Gear.D && sample.speedMps > STOP_MPS) {
                resetTracking()
                _live.value = LiveDrive(state = DriveState.DRIVING, startedAtMs = sample.timestampMs)
                last = sample
                checkRules(sample, segment, 0.0, judgeChange = false) // 첫 샘플은 1초 변화량 판정 안 함
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
                if (ended) closeEpisode(distanceKm) else checkRules(sample, segment, distanceKm, judgeChange = true)
            }
        }
    }

    /** 외부 이벤트 주입 (안전거리 미확보: ADAS 미지원이라 데모만). 주행 중일 때만 기록. */
    fun injectViolation(type: ViolationType, locationLabel: String = currentLabel) {
        val cur = _live.value
        if (cur.state != DriveState.DRIVING) return
        addViolation(Violation.momentary(type, cur.distanceKm, (last?.speedMps ?: 0.0) * 3.6, locationLabel))
    }

    /** 적립 완료 후 대기 상태로. */
    fun reset() {
        resetTracking()
        history.clear()
        _live.value = LiveDrive()
    }

    private fun remember(sample: VehicleSample) {
        val prev = history.lastOrNull()
        if (prev != null && sample.timestampMs - prev.timestampMs > HISTORY_MS) history.clear() // 데이터 끊김
        history.addLast(sample)
        while (history.first().timestampMs < sample.timestampMs - HISTORY_MS) history.removeFirst()
    }

    private fun checkRules(sample: VehicleSample, segment: RoadSegment, atKm: Double, judgeChange: Boolean) {
        val kmh = sample.speedMps * 3.6
        currentLabel = segment.locationLabel
        checkOverspeed(sample.timestampMs, kmh, segment, atKm)
        if (judgeChange) checkSpeedChange(sample, kmh, segment.locationLabel, atKm)

        // 급커브: 커브 구간에 "진입하는 순간" 속도가 40km/h 초과면 1건
        if (segment.isCurve && segment != lastSegment && kmh > CURVE_MAX_KMH + EPS_KMH) {
            addViolation(Violation.momentary(ViolationType.SHARP_CURVE, atKm, kmh, segment.locationLabel))
        }
        lastSegment = segment
    }

    /**
     * 과속 에피소드: 제한+10 "초과"가 3초 지속되면 확정(3초 미만은 버림).
     * 내려온 뒤 3초 안에 다시 초과하면 같은 에피소드, 3초 지나면 처음 내려온 지점에서 마감.
     */
    private fun checkOverspeed(t: Long, kmh: Double, segment: RoadSegment, atKm: Double) {
        val overKmh = kmh - segment.limitKmh
        val ep = episode
        if (overKmh > OVERSPEED_MARGIN_KMH + EPS_KMH) { // EPS: 60/3.6*3.6 같은 부동소수 오차
            val e = ep ?: Episode(t, atKm, segment.locationLabel).also { episode = it }
            e.belowSinceMs = null
            e.maxOverKmh = maxOf(e.maxOverKmh, overKmh)
            e.maxSpeedKmh = maxOf(e.maxSpeedKmh, kmh)
            if (t - e.startMs >= OVERSPEED_HOLD_MS) e.confirmed = true
        } else if (ep != null) {
            if (!ep.confirmed) {
                episode = null
            } else {
                val since = ep.belowSinceMs ?: t.also { ep.belowSinceMs = it; ep.belowKm = atKm }
                if (t - since >= OVERSPEED_HOLD_MS) closeEpisode(ep.belowKm)
            }
        }
    }

    /** 주행 종료(P) 또는 정상 속도 3초 경과 시 에피소드 마감 */
    private fun closeEpisode(endKm: Double) {
        val ep = episode ?: return
        episode = null
        if (!ep.confirmed) return
        val end = if (ep.belowSinceMs != null) ep.belowKm else endKm
        addViolation(Violation.overspeed(ep.startKm, end, ep.maxOverKmh, ep.maxSpeedKmh, ep.label))
    }

    /** 급출발·급가속·급감속·급정지 (교통안전공단 eTAS 기준, 1초 속도 변화량) */
    private fun checkSpeedChange(sample: VehicleSample, kmh: Double, label: String, atKm: Double) {
        val t = sample.timestampMs
        val ref = history.lastOrNull { it.timestampMs <= t - 1000 } ?: return // 1초 이력 없음
        val v1s = ref.speedMps * 3.6
        val dv = kmh - v1s
        if (dv > SPIKE_UP_KMH || dv < SPIKE_DOWN_KMH) return // 센서 튐

        if (dv >= ACCEL_KMH - EPS_KMH) {
            val bundled = lastAccelMs?.let { t - it <= ACCEL_BUNDLE_MS } == true
            lastAccelMs = t
            if (!bundled) {
                val type = if (v1s <= START_FROM_KMH) ViolationType.HARD_START else ViolationType.HARD_ACCEL
                addViolation(Violation.momentary(type, atKm, v1s, label))
            }
        } else if (dv <= DECEL_KMH + EPS_KMH) {
            val stopped = sample.speedMps <= STOP_MPS
            val bundled = lastDecelMs?.let { t - it <= DECEL_BUNDLE_MS } == true
            lastDecelMs = t
            if (bundled) {
                // 같은 묶음에서 정지까지 이어지면 급감속 → 급정지로 바꿈
                val i = decelIndex ?: return
                val v = _live.value.violations[i]
                if (stopped && v.type == ViolationType.HARD_DECEL) replaceViolation(i, v.copy(type = ViolationType.HARD_STOP))
            } else {
                val type = if (stopped) ViolationType.HARD_STOP else ViolationType.HARD_DECEL
                // 주행당 첫 급감속/급정지는 유예 (끼어들기 오탐 대비, 팀 정책)
                addViolation(Violation.momentary(type, atKm, v1s, label, waived = !decelSeen))
                decelSeen = true
                decelIndex = _live.value.violations.lastIndex
            }
        }
    }

    private fun addViolation(v: Violation) {
        _live.value = _live.value.let { it.copy(violations = it.violations + v) }
    }

    private fun replaceViolation(i: Int, v: Violation) {
        _live.value = _live.value.let { it.copy(violations = it.violations.toMutableList().also { l -> l[i] = v }) }
    }

    private fun resetTracking() {
        last = null
        lastSegment = null
        currentLabel = ""
        lastAccelMs = null
        lastDecelMs = null
        decelIndex = null
        decelSeen = false
        episode = null
    }

    companion object {
        const val STOP_MPS = 0.1
        const val OVERSPEED_MARGIN_KMH = 10.0
        const val OVERSPEED_HOLD_MS = 3_000L
        const val CURVE_MAX_KMH = 40.0

        /** eTAS 11대 위험운전: 급가속·급출발 초당 +11, 급감속·급정지 초당 −7.5 */
        const val ACCEL_KMH = 11.0
        const val DECEL_KMH = -7.5
        /** 급출발: 1초 전 속도가 이 이하 */
        const val START_FROM_KMH = 5.0
        /** 센서 튐: 공단 기준 범위 상한 밖은 무시 */
        const val SPIKE_UP_KMH = 25.0
        const val SPIKE_DOWN_KMH = -40.0
        /** 연속 이벤트 묶음 (팀 정책, 공단 eTAS 처리 방식) */
        const val ACCEL_BUNDLE_MS = 3_000L
        const val DECEL_BUNDLE_MS = 1_000L
        /** 1초 변화량용 이력 길이. 샘플 간격이 이보다 비면 이력을 비운다 */
        const val HISTORY_MS = 1_500L
        private const val EPS_KMH = 0.01
    }
}
