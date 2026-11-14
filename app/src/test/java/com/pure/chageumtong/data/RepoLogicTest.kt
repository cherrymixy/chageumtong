package com.pure.chageumtong.data

import com.pure.chageumtong.data.repo.AppData
import com.pure.chageumtong.data.repo.PurchaseResult
import com.pure.chageumtong.data.repo.adBonus
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

    /** 리포 로직 검증용 500m 구간 위반 */
    private fun v500(atKm: Double) = Violation(ViolationType.SAFE_DISTANCE, atKm, atKm - 0.25, atKm + 0.25, "")

    private val product = Product("p1", ShopTab.SHOP, "테스트", "1,000원", 1000, "x")

    @Test fun `같은 세션은 한 번만 적립`() {
        val s = session("a", 17.4, violations = listOf(v500(3.0), v500(9.0)))
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

    @Test fun `최근 30일과 이번 주 통계`() {
        val d = AppData()
            .claim(session("old", 10.0, ms(2026, 10, 14)), now)!! // 30일 밖 (10/15부터 포함)
            .claim(session("edge", 2.0, ms(2026, 10, 15, 0)), now)!! // 30일 창 첫날
            .claim(session("mon", 5.0, ms(2026, 11, 9)), now)!! // 이번 주 월
            .claim(session("fri", 12.0, ms(2026, 11, 13), listOf(v500(6.0))), now)!!
        val m = d.monthStats(now, zone)
        assertEquals(19.0, m.distanceKm, 1e-9)
        assertEquals(18.5, m.safeKm, 1e-9)
        assertEquals(97, m.safeRatio)
        assertEquals(10 + 2 + 5 + 11, m.earned) // 적립 거래 시각(now)이 모두 30일 안
        assertEquals(listOf(5.0, 0.0, 0.0, 0.0, 12.0, 0.0, 0.0), d.weekDailyKm(now, zone))
    }

    @Test fun `시드 총합 14560P, 4주 출퇴근, 안전비율 90 이상`() {
        val seed = SeedData.create(now, zone, products)
        assertEquals(14_560, seed.totalPoints)
        assertTrue(seed.seeded)
        assertEquals(setOf("bl-vgs", "bl-lighting", "bl-theme-unicorn"), seed.ownedProductIds)
        assertEquals(3, seed.txs.count { it.amount < 0 })
        assertTrue(seed.sessions.size in 40..48) // 평일 20일 × 2 + 주말 8일 × 0~1
        assertEquals(seed.sessions.size, seed.sessions.map { it.id }.toSet().size)
        assertTrue(seed.sessions.all { it.safeRatio in 90..100 && it.distanceKm in 20.0..45.0 })
        // 시드 주행 적립은 모두 최근 30일, 보정 거래는 섞이지 않음
        assertEquals(seed.txs.filter { it.sessionId != null }.sumOf { it.amount }, seed.monthStats(now, zone).earned)
        // 같은 입력이면 같은 시드
        assertEquals(seed, SeedData.create(now, zone, products))
    }

    @Test fun `광고 보너스는 세션 없는 적립 거래로 더해진다`() {
        val d = AppData().adBonus(5, now)
        assertEquals(5, d.totalPoints)
        assertNull(d.txs.single().sessionId)
    }
}
