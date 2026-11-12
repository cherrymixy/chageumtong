package com.pure.chageumtong.domain

import kotlin.math.floor
import kotlin.math.roundToInt

/** 위반 지점 앞뒤 거리 (PRD §12 D1: ±250m) */
const val VIOLATION_HALF_KM = 0.25

/** 이보다 짧은 주행은 적립 불가 (PRD §3) */
const val MIN_CLAIM_KM = 0.3

/** 미적립 구간 합집합 길이. 겹치는 구간은 한 번만, [0, distanceKm] 밖은 잘라낸다. */
fun unsafeKm(violations: List<Violation>, distanceKm: Double): Double {
    val ranges = violations
        .map { it.segmentStartKm.coerceAtLeast(0.0) to it.segmentEndKm.coerceAtMost(distanceKm) }
        .filter { (s, e) -> s < e }
        .sortedBy { it.first }
    var total = 0.0
    var curStart = Double.NaN
    var curEnd = Double.NaN
    for ((s, e) in ranges) {
        if (curStart.isNaN() || s > curEnd) {
            if (!curStart.isNaN()) total += curEnd - curStart
            curStart = s
            curEnd = e
        } else if (e > curEnd) {
            curEnd = e
        }
    }
    if (!curStart.isNaN()) total += curEnd - curStart
    return total
}

fun safeKm(distanceKm: Double, violations: List<Violation>): Double =
    (distanceKm - unsafeKm(violations, distanceKm)).coerceAtLeast(0.0)

/** 안전운전 비율(%) = 안전운전거리 / 총 이동거리. 이동거리 0이면 0 (대기 상태 리셋 표시). */
fun safeRatio(distanceKm: Double, violations: List<Violation>): Int =
    if (distanceKm <= 0.0) 0 else (safeKm(distanceKm, violations) / distanceKm * 100).roundToInt()

/** 1km = 1P, 소수점 버림. 1e-9는 16.9999999 같은 부동소수 오차 보정. */
fun points(distanceKm: Double, violations: List<Violation>): Int =
    floor(safeKm(distanceKm, violations) + 1e-9).toInt()

fun canClaim(distanceKm: Double): Boolean = distanceKm >= MIN_CLAIM_KM

fun praise(ratio: Int): String = when {
    ratio >= 95 -> "참 잘했어요!"
    ratio >= 85 -> "좋아요!"
    ratio >= 70 -> "조금만 더!"
    else -> "다음엔 더 안전하게"
}
