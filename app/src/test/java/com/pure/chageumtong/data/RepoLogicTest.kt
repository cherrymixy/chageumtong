package com.pure.chageumtong.data

import com.pure.chageumtong.data.repo.AppData
import com.pure.chageumtong.data.repo.PurchaseResult
import com.pure.chageumtong.data.repo.claim
import com.pure.chageumtong.data.repo.monthStats
import com.pure.chageumtong.data.repo.purchase
import com.pure.chageumtong.data.repo.weekDailyKm
import com.pure.chageumtong.data.seed.SeedData
import com.pure.chageumtong.domain.DriveSession
import com.pure.chageumtong.domain.PointTx
import com.pure.chageumtong.domain.Product
import com.pure.chageumtong.domain.ShopTab
import com.pure.chageumtong.domain.Violation
import com.pure.chageumtong.domain.ViolationType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

class RepoLogicTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private fun ms(y: Int, mo: Int, d: Int, h: Int = 12) = LocalDateTime.of(y, mo, d, h, 0).atZone(zone).toInstant().toEpochMilli()
    private val now = ms(2026, 11, 13) // 금요일
    private val products: List<Product> = Json.decodeFromString(File("src/main/assets/products.json").readText())

    private fun session(id: String, km: Double, at: Long = now, violations: List<Violation> = emptyList()) =
        DriveSession(id, at, at + 60_000, km, 60, violations, null, claimed = false)

    private val product = Product("p1", ShopTab.SHOP, "테스트", "1,000원", 1000, "x")

    @Test fun `같은 세션은 한 번만 적립`() {
        val s = session("a", 17.4, violations = listOf(Violation.at(ViolationType.SAFE_DISTANCE, 3.0, ""), Violation.at(ViolationType.SAFE_DISTANCE, 9.0, "")))
        val once = AppData().claim(s, now)
        assertNotNull(once)
        assertEquals(16, once!!.totalPoints)
        assertNull(once.claim(s, now))
        assertEquals(1, once.sessions.size)
    }

    @Test fun `0점3km 미만은 적립 거부, 0P 주행은 기록만`() {
        assertNull(AppData().claim(session("short", 0.29), now))
        val zero = AppData().claim(session("zero", 0.74), now)!!
        assertEquals(0, zero.totalPoints)
        assertEquals(1, zero.sessions.size)
        assertTrue(zero.txs.isEmpty())
    }

    @Test fun `잔액 부족이면 구매 실패하고 그대로`() {
        val d = AppData(txs = listOf(PointTx("t", now, 400, null, null, "")))
        val (next, r) = d.purchase(product, now)
        assertEquals(PurchaseResult.Insufficient(600), r)
        assertEquals(d, next)
    }

    @Test fun `구매 성공 시 차감과 보유, 재구매는 보유중`() {
        val d = AppData(txs = listOf(PointTx("t", now, 1500, null, null, "")))
        val (next, r) = d.purchase(product, now)
        assertEquals(PurchaseResult.Success, r)
        assertEquals(500, next.totalPoints)
        assertTrue("p1" in next.ownedProductIds)
        assertEquals(PurchaseResult.AlreadyOwned, next.purchase(product, now).second)
    }

    @Test fun `이번 달과 이번 주 통계`() {
        val d = AppData()
            .claim(session("oct", 10.0, ms(2026, 10, 31)), now)!! // 지난달
            .claim(session("mon", 5.0, ms(2026, 11, 9)), now)!! // 이번 주 월
            .claim(session("fri", 12.0, ms(2026, 11, 13), listOf(Violation.at(ViolationType.SAFE_DISTANCE, 6.0, ""))), now)!!
        val m = d.monthStats(now, zone)
        assertEquals(17.0, m.distanceKm, 1e-9)
        assertEquals(16.5, m.safeKm, 1e-9)
        assertEquals(97, m.safeRatio)
        assertEquals(10 + 5 + 11, m.earned) // 적립 거래 시각(now)이 모두 이번 달
        assertEquals(listOf(5.0, 0.0, 0.0, 0.0, 12.0, 0.0, 0.0), d.weekDailyKm(now, zone))
    }

    @Test fun `시드 총합 14560P, 4주 주 4~6회, 안전비율 85 이상`() {
        val seed = SeedData.create(now, zone, products)
        assertEquals(14_560, seed.totalPoints)
        assertTrue(seed.seeded)
        assertEquals(setOf("bl-vgs", "bl-lighting", "bl-theme-unicorn"), seed.ownedProductIds)
        assertEquals(3, seed.txs.count { it.amount < 0 })
        assertTrue(seed.sessions.size in 16..24)
        assertTrue(seed.sessions.all { it.safeRatio in 85..100 && it.distanceKm in 5.0..30.0 })
        // 보정 거래가 이번 달 적립에 섞이지 않음
        assertEquals(seed.txs.filter { it.sessionId != null && it.at >= ms(2026, 11, 1, 0) }.sumOf { it.amount }, seed.monthStats(now, zone).earned)
        // 같은 입력이면 같은 시드
        assertEquals(seed, SeedData.create(now, zone, products))
    }
}
