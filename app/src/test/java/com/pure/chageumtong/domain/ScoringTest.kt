package com.pure.chageumtong.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {
    /** 60km/h 순간 행동 = 정지시거 75m → 지점 ±37.5m */
    private fun v(atKm: Double) = Violation.momentary(ViolationType.SAFE_DISTANCE, atKm, 60.0, "")

    @Test fun `정지시거 경계`() {
        mapOf(15.0 to 20, 20.0 to 20, 59.0 to 55, 60.0 to 75, 85.0 to 110, 120.0 to 215, 150.0 to 215)
            .forEach { (kmh, m) -> assertEquals("$kmh", m, stoppingSightM(kmh)) }
    }

    @Test fun `등급 경계`() {
        assertEquals(1, gradeForOverKmh(20.0))
        assertEquals(2, gradeForOverKmh(20.5))
        assertEquals(2, gradeForOverKmh(40.0))
        assertEquals(3, gradeForOverKmh(60.0))
        assertEquals(4, gradeForOverKmh(60.5))
    }

    @Test fun `60km에서 급정지 1등급`() {
        val s = Violation.momentary(ViolationType.HARD_STOP, 5.0, 60.0, "")
        assertEquals(4.9625, s.segmentStartKm, 1e-9)
        assertEquals(5.0375, s.segmentEndKm, 1e-9)
        assertEquals(0.075, unsafeKm(listOf(s), 10.0), 1e-9)
    }

    @Test fun `85km 과속 2등급은 정지시거x2 버퍼`() {
        val o = Violation.overspeed(2.0, 2.4, 25.0, 85.0, "")
        assertEquals(2, o.grade)
        assertEquals(1.89, o.segmentStartKm, 1e-9)
        assertEquals(2.51, o.segmentEndKm, 1e-9)
        assertEquals(0.62, unsafeKm(listOf(o), 10.0), 1e-9)
    }

    @Test fun `슬라이드 예시 20km 19P`() {
        val list = listOf(
            Violation.momentary(ViolationType.HARD_STOP, 5.0, 60.0, ""),
            Violation.overspeed(2.0, 2.4, 25.0, 85.0, ""),
        )
        assertEquals(0.695, unsafeKm(list, 20.0), 1e-9)
        assertEquals(19.305, safeKm(20.0, list), 1e-9)
        assertEquals(19, points(20.0, list))
    }

    @Test fun `1등급 과속은 버퍼 없이 과속 구간만`() {
        val o = Violation.overspeed(3.0, 3.3, 15.0, 75.0, "")
        assertEquals(1, o.grade)
        assertEquals(0.3, unsafeKm(listOf(o), 10.0), 1e-9)
    }

    @Test fun `4등급 과속은 주행 전체 미적립`() {
        val o = Violation.overspeed(3.0, 3.1, 65.0, 125.0, "")
        assertTrue(o.voidsTrip)
        assertEquals(0, points(20.0, listOf(o)))
        assertEquals(0, safeRatio(20.0, listOf(o)))
    }

    @Test fun `유예 건은 차감 안 함`() {
        val w = Violation.momentary(ViolationType.HARD_DECEL, 5.0, 60.0, "", waived = true)
        assertEquals(0.0, unsafeKm(listOf(w), 10.0), 1e-9)
        assertEquals(10, points(10.0, listOf(w)))
    }

    @Test fun `겹치는 구간은 합집합`() {
        // 5.0±0.0375 와 5.05±0.0375 → 4.9625~5.0875 = 0.125
        assertEquals(0.125, unsafeKm(listOf(v(5.05), v(5.0)), 10.0), 1e-9)
    }

    @Test fun `떨어진 구간은 각각 더함`() = assertEquals(0.15, unsafeKm(listOf(v(2.0), v(8.0)), 10.0), 1e-9)

    @Test fun `주행거리 범위 밖은 잘라냄`() {
        assertEquals(0.0375, unsafeKm(listOf(v(0.0)), 10.0), 1e-9) // 시작점
        assertEquals(0.0475, unsafeKm(listOf(v(9.99)), 10.0), 1e-9) // 9.9525~10.0
    }

    @Test fun `정수 경계 부동소수 오차`() = assertEquals(17, points(17.075, listOf(v(3.0))))

    @Test fun `0점3km 경계`() {
        assertFalse(canClaim(0.299))
        assertTrue(canClaim(0.3))
    }

    @Test fun `이동거리 0이면 비율 0`() = assertEquals(0, safeRatio(0.0, emptyList()))

    @Test fun `새 필드 없는 예전 세션 JSON도 읽힘`() {
        val old = """{"id":"d","startedAt":0,"endedAt":1,"distanceKm":10.0,"durationSec":600,
            "violations":[{"type":"SAFE_DISTANCE","atKm":5.0,"segmentStartKm":4.75,"segmentEndKm":5.25,"locationLabel":"x"}],
            "routeId":null,"claimed":true}"""
        val s = Json.decodeFromString<DriveSession>(old)
        assertEquals(1, s.violations.single().grade)
        assertFalse(s.violations.single().waived)
        assertEquals(9, s.points)
    }

    @Test fun `칭찬 문구 경계`() {
        assertEquals("참 잘했어요!", praise(95))
        assertEquals("좋아요!", praise(94))
        assertEquals("좋아요!", praise(85))
        assertEquals("조금만 더!", praise(84))
        assertEquals("조금만 더!", praise(70))
        assertEquals("다음엔 더 안전하게", praise(69))
    }
}
