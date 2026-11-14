package com.pure.chageumtong.domain

import kotlin.math.floor
import kotlin.math.roundToInt

/** 정지시거(m), 도로의 구조·시설 기준에 관한 규칙 제24조. 키 = 설계속도(km/h) */
private val STOPPING_SIGHT_M = mapOf(
    20 to 20, 30 to 30, 40 to 40, 50 to 55, 60 to 75, 70 to 95,
    80 to 110, 90 to 130, 100 to 155, 110 to 185, 120 to 215,
)

/** 속도를 10km/h 단위로 내려 표에서 찾는다. 20 미만은 20m, 120 초과는 215m. */
fun stoppingSightM(kmh: Double): Int =
    STOPPING_SIGHT_M.getValue((floor(kmh / 10).toInt() * 10).coerceIn(20, 120))

/** 과속 초과폭 → 등급 (도로교통법 시행규칙 별표 28 벌점 구간). 10 초과~20 이하 = 1 */
fun gradeForOverKmh(overKmh: Double): Int = when {
    overKmh <= 20.0 -> 1
    overKmh <= 40.0 -> 2
    overKmh <= 60.0 -> 3
    else -> 4
}

/** 등급별 정지시거 계수 (4등급은 주행 전체 미적립이라 계수 없음) */
fun coefForGrade(grade: Int): Int = when (grade) {
    2 -> 2
    3 -> 4
    else -> 1
}

/** 차감 대상: 유예 건은 뺀다 */
private fun List<Violation>.counted() = filterNot { it.waived }

/** 4등급 과속이 있으면 그 주행은 전체 미적립 */
fun voidsTrip(violations: List<Violation>): Boolean = violations.counted().any { it.voidsTrip }

/** 이보다 짧은 주행은 적립 불가 (PRD §3) */
const val MIN_CLAIM_KM = 0.3

/** 미적립 구간 합집합 길이. 겹치는 구간은 한 번만, [0, distanceKm] 밖은 잘라낸다. */
fun unsafeKm(violations: List<Violation>, distanceKm: Double): Double {
    val ranges = violations.counted()
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
    if (voidsTrip(violations)) 0.0 else (distanceKm - unsafeKm(violations, distanceKm)).coerceAtLeast(0.0)

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
