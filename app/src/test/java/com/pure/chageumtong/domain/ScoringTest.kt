package com.pure.chageumtong.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {
    private fun v(atKm: Double) = Violation.at(ViolationType.SAFE_DISTANCE, atKm, "")

    @Test fun `구간 하나는 500m`() = assertEquals(0.5, unsafeKm(listOf(v(5.0)), 10.0), 1e-9)

    @Test fun `겹치는 구간은 합집합`() {
        // 5.0±0.25 와 5.3±0.25 → 4.75~5.55 = 0.8
        assertEquals(0.8, unsafeKm(listOf(v(5.3), v(5.0)), 10.0), 1e-9)
    }

    @Test fun `떨어진 구간은 각각 더함`() = assertEquals(1.0, unsafeKm(listOf(v(2.0), v(8.0)), 10.0), 1e-9)

    @Test fun `주행거리 범위 밖은 잘라냄`() {
        assertEquals(0.25, unsafeKm(listOf(v(0.0)), 10.0), 1e-9) // 시작점
        assertEquals(0.35, unsafeKm(listOf(v(9.9)), 10.0), 1e-9) // 9.65~10.0
    }

    @Test fun `15점4km 위반 1건이면 14P`() {
        val s = listOf(v(7.0))
        assertEquals(14.9, safeKm(15.4, s), 1e-9)
        assertEquals(14, points(15.4, s))
        assertEquals(97, safeRatio(15.4, s))
    }

    @Test fun `17점4km 미적립 1km면 16P`() = assertEquals(16, points(17.4, listOf(v(3.0), v(9.0))))

    @Test fun `정수 경계 부동소수 오차`() = assertEquals(17, points(17.5, listOf(v(3.0))))

    @Test fun `0점3km 경계`() {
        assertFalse(canClaim(0.299))
        assertTrue(canClaim(0.3))
    }

    @Test fun `이동거리 0이면 비율 0`() = assertEquals(0, safeRatio(0.0, emptyList()))

    @Test fun `칭찬 문구 경계`() {
        assertEquals("참 잘했어요!", praise(95))
        assertEquals("좋아요!", praise(94))
        assertEquals("좋아요!", praise(85))
        assertEquals("조금만 더!", praise(84))
        assertEquals("조금만 더!", praise(70))
        assertEquals("다음엔 더 안전하게", praise(69))
    }
}
