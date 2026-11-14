package com.pure.chageumtong.ui

import com.pure.chageumtong.data.vehicle.DriveInput
import com.pure.chageumtong.domain.DriveSessionEngine
import com.pure.chageumtong.domain.VehicleSample

/** 홈 일러스트 연출용 차량 움직임 (점수와 무관, 화면 효과만) */
data class DriveMotion(val moving: Boolean = false, val overspeed: Boolean = false)

fun DriveInput?.toMotion(): DriveMotion {
    val input = this ?: return DriveMotion()
    val s = input.sample
    val kmh = s.speedMps * 3.6
    return DriveMotion(
        moving = s.speedMps > DriveSessionEngine.STOP_MPS,
        // 엔진과 같은 기준(제한 +10 초과). 연출이라 3초 지속은 기다리지 않음
        overspeed = kmh > input.segment.limitKmh + DriveSessionEngine.OVERSPEED_MARGIN_KMH + 0.01, // 0.01: 부동소수 오차
    )
}

/**
 * 급정거 감지: WINDOW_MS 이상 떨어진 두 샘플 사이 감속도가 기준 이상이면 true.
 * 실차 샘플은 촘촘해서 바로 옆 샘플끼리 비교하면 흔들리므로 창을 둔다.
 */
class HardBrakeDetector(
    private val thresholdMps2: Double = 3.0, // 약 0.3g, 차량 텔레매틱스에서 흔히 쓰는 급감속 기준
    private val windowMs: Long = 500,
) {
    private var ref: VehicleSample? = null

    fun onSample(s: VehicleSample): Boolean {
        val r = ref
        if (r == null || s.timestampMs < r.timestampMs) { ref = s; return false } // 첫 샘플 / 데모 재시작
        val dt = s.timestampMs - r.timestampMs
        if (dt < windowMs) return false
        ref = s
        return (r.speedMps - s.speedMps) * 1000.0 / dt >= thresholdMps2
    }
}
