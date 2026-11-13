package com.pure.chageumtong.data.repo

import com.pure.chageumtong.domain.DriveSession
import com.pure.chageumtong.domain.PointTx
import com.pure.chageumtong.domain.Product
import com.pure.chageumtong.domain.canClaim
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/** 앱에 저장되는 전부. DataStore 파일 하나에 통째로 저장해 적립·차감이 항상 함께 반영된다. */
@Serializable
data class AppData(
    val sessions: List<DriveSession> = emptyList(),
    val txs: List<PointTx> = emptyList(),
    val ownedProductIds: Set<String> = emptySet(),
    val favoritePlaceIds: Set<String> = emptySet(),
    val seeded: Boolean = false,
    /** 마이페이지 설정: 적립 완료 음성 안내 */
    val ttsOn: Boolean = true,
) {
    val totalPoints: Int get() = txs.sumOf { it.amount }
}

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data class Insufficient(val shortBy: Int) : PurchaseResult
    data object AlreadyOwned : PurchaseResult
}

/** 세션 ID당 1회만 적립. 적립 불가(0.3km 미만)거나 이미 적립했으면 null. 0P여도 주행 기록은 남긴다. */
fun AppData.claim(session: DriveSession, now: Long): AppData? {
    if (sessions.any { it.id == session.id && it.claimed }) return null
    if (!canClaim(session.distanceKm)) return null
    val claimed = session.copy(claimed = true)
    val tx = if (claimed.points > 0) {
        listOf(PointTx("tx-${session.id}", now, claimed.points, session.id, null, "주행 적립"))
    } else {
        emptyList()
    }
    return copy(sessions = sessions.filterNot { it.id == session.id } + claimed, txs = txs + tx)
}

/** 가격 = 포인트 1:1 (PRD §6.4) */
fun AppData.purchase(product: Product, now: Long): Pair<AppData, PurchaseResult> = when {
    product.id in ownedProductIds -> this to PurchaseResult.AlreadyOwned
    totalPoints < product.price -> this to PurchaseResult.Insufficient(product.price - totalPoints)
    else -> copy(
        txs = txs + PointTx("tx-buy-${product.id}-$now", now, -product.price, null, product.id, product.name),
        ownedProductIds = ownedProductIds + product.id,
    ) to PurchaseResult.Success
}

/** N모드 정차 중 광고 시청 보너스 (Figma 팝업 105:2307). 주행과 무관한 거래로 남는다 */
fun AppData.adBonus(points: Int, now: Long): AppData =
    copy(txs = txs + PointTx("tx-ad-$now", now, points, null, null, "광고 시청 적립"))

data class MonthStats(val earned: Int, val distanceKm: Double, val safeKm: Double, val safeRatio: Int)

private fun Long.toDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/**
 * 최근 30일(오늘 포함): 적립 포인트(+거래만), 주행거리, 안전운전 거리, 안전운전 비율.
 * 달력 월이면 월초(예: PT 12/3)에 거의 비어 보여서 30일 창으로 (STEP 11, 승아 결정)
 */
fun AppData.monthStats(now: Long, zone: ZoneId): MonthStats {
    val from = now.toDate(zone).minusDays(29)
    fun inMonth(t: Long) = !t.toDate(zone).isBefore(from)
    val monthSessions = sessions.filter { inMonth(it.startedAt) }
    val distance = monthSessions.sumOf { it.distanceKm }
    val safe = monthSessions.sumOf { it.safeKm }
    return MonthStats(
        earned = txs.filter { it.amount > 0 && inMonth(it.at) }.sumOf { it.amount },
        distanceKm = distance,
        safeKm = safe,
        safeRatio = if (distance <= 0) 0 else (safe / distance * 100).roundToInt(),
    )
}

/** 이번 주 월~일 요일별 주행거리 (7개) */
fun AppData.weekDailyKm(now: Long, zone: ZoneId): List<Double> {
    val monday = now.toDate(zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0L..6L).map { offset ->
        val day = monday.plusDays(offset)
        sessions.filter { it.startedAt.toDate(zone) == day }.sumOf { it.distanceKm }
    }
}
