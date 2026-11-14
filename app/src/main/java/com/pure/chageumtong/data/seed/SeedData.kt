package com.pure.chageumtong.data.seed

import com.pure.chageumtong.data.repo.AppData
import com.pure.chageumtong.domain.DEFAULT_LIMIT_KMH
import com.pure.chageumtong.domain.DriveSession
import com.pure.chageumtong.domain.PointTx
import com.pure.chageumtong.domain.Product
import com.pure.chageumtong.domain.Violation
import com.pure.chageumtong.domain.ViolationType
import java.time.Instant
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * 첫 실행 시드: 지난 4주 출퇴근 주행(평일 2회·주말 0~1회, 20~45km, 안전비율 90~100%) + 사용 내역 3건.
 * 마지막에 "이전 적립 포인트" 보정 거래로 총합을 정확히 114,560P로 맞춘다 (PRD §8, 시연에서 상점 상품을 살 수 있게 14,560 → 114,560).
 */
object SeedData {
    const val TOTAL_POINTS = 114_560
    private const val RANDOM_SEED = 20261115
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** 사용 내역 (상품 ID, 며칠 전) — 실제로 보유 처리된다 */
    private val usages = listOf("bl-vgs" to 20, "bl-lighting" to 12, "bl-theme-unicorn" to 5)
    private val labels = listOf("종로구 1-4", "중구 5-1", "용산구 3-4", "마포구 7-2", "강남구 12-8", "성동구 4-11", "서초구 2-6")

    fun create(now: Long, zone: ZoneId, products: List<Product>): AppData {
        val rnd = Random(RANDOM_SEED)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

        val sessions = mutableListOf<DriveSession>()
        // 출퇴근 차량: 평일(1~28일 전)은 출근·퇴근 2회, 주말은 절반 확률로 나들이 1회
        for (daysAgo in 1..28) {
            val day = today.minusDays(daysAgo.toLong())
            val hours = if (day.dayOfWeek.value <= 5) listOf(rnd.nextInt(7, 9), rnd.nextInt(18, 20))
            else if (rnd.nextBoolean()) listOf(rnd.nextInt(10, 16)) else emptyList()
            hours.forEachIndexed { i, h ->
                val start = day.atTime(h, rnd.nextInt(0, 60)).atZone(zone).toInstant().toEpochMilli()
                sessions += session("seed-$daysAgo-$i", start, rnd)
            }
        }
        val claimTxs = sessions.filter { it.points > 0 }.map {
            PointTx("tx-${it.id}", it.endedAt!!, it.points, it.id, null, "주행 적립")
        }

        val byId = products.associateBy { it.id }
        val usageTxs = usages.mapNotNull { (id, daysAgo) ->
            byId[id]?.let { p -> PointTx("tx-seed-buy-$id", now - daysAgo * DAY_MS, -p.price, null, id, p.name) }
        }

        val sum = (claimTxs + usageTxs).sumOf { it.amount }
        // 보정 거래는 30일보다 이전 시각으로 → "최근 30일 적립" 통계에 안 섞인다
        val adjust = PointTx("tx-seed-carryover", now - 60 * DAY_MS, TOTAL_POINTS - sum, null, null, "이전 적립 포인트")

        return AppData(
            sessions = sessions,
            txs = (listOf(adjust) + claimTxs + usageTxs).sortedBy { it.at },
            ownedProductIds = usageTxs.mapNotNull { it.productId }.toSet(),
            seeded = true,
        )
    }

    private fun session(id: String, start: Long, rnd: Random): DriveSession {
        val distance = floor((20 + rnd.nextDouble() * 25) * 10) / 10 // 20.0~45.0km (출퇴근)
        // 위반 1건 = 수십~수백 m. 0~4건 → 안전비율 90% 이상
        val count = rnd.nextInt(0, 5)
        val gap = distance / (count + 1) // 등간격 배치 → 구간이 겹치지 않음
        val violations = (1..count).map { i ->
            val type = ViolationType.entries[rnd.nextInt(ViolationType.entries.size)]
            val atKm = floor(gap * i * 100) / 100
            val label = labels[rnd.nextInt(labels.size)]
            if (type == ViolationType.OVERSPEED) {
                val over = rnd.nextInt(12, 26).toDouble()
                Violation.overspeed(atKm, atKm + 0.2, over, DEFAULT_LIMIT_KMH + over, label)
            } else {
                Violation.momentary(type, atKm, DEFAULT_LIMIT_KMH.toDouble(), label)
            }
        }
        val avgKmh = rnd.nextInt(30, 51)
        val durationSec = (distance / avgKmh * 3600).roundToLong()
        return DriveSession(
            id = id,
            startedAt = start,
            endedAt = start + durationSec * 1000,
            distanceKm = distance,
            durationSec = durationSec,
            violations = violations,
            routeId = null,
            claimed = true,
        )
    }
}
