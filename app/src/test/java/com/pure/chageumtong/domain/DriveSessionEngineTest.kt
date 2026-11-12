package com.pure.chageumtong.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveSessionEngineTest {
    private val engine = DriveSessionEngine()
    private var t = 0L

    /** 1초 간격 샘플을 seconds개 보낸다. */
    private fun drive(kmh: Double, seconds: Int, segment: RoadSegment = RoadSegment(), gear: Gear = Gear.D) {
        repeat(seconds) {
            engine.onSample(VehicleSample(t, kmh / 3.6, gear), segment)
            t += 1000
        }
    }

    private fun park() = engine.onSample(VehicleSample(t, 0.0, Gear.P))
    private val live get() = engine.live.value

    @Test fun `D와 속도로 시작, 정차+P로 종료`() {
        drive(0.0, 3) // D지만 정지 → 대기
        assertEquals(DriveState.IDLE, live.state)
        drive(36.0, 101) // 10m/s × 100초 = 1km
        assertEquals(DriveState.DRIVING, live.state)
        drive(0.0, 1) // 정지했지만 아직 D
        assertEquals(DriveState.DRIVING, live.state)
        park()
        assertEquals(DriveState.ENDED, live.state)
        assertEquals(1.0 + 0.005, live.distanceKm, 1e-6) // 마지막 감속 구간 사다리꼴 5m
        assertTrue(live.canClaim)
    }

    @Test fun `0점3km 미만이면 적립 불가`() {
        drive(36.0, 20) // 190m
        drive(0.0, 1)
        park()
        assertEquals(DriveState.ENDED, live.state)
        assertEquals(false, live.canClaim)
    }

    @Test fun `정확히 제한+10은 과속 아님`() {
        drive(70.0, 30, RoadSegment(limitKmh = 60))
        assertTrue(live.violations.isEmpty())
    }

    @Test fun `3초 미만 과속은 위반 아님`() {
        drive(75.0, 3, RoadSegment(limitKmh = 60)) // 0,1,2초 → 지속 2초
        drive(60.0, 5)
        assertTrue(live.violations.isEmpty())
    }

    @Test fun `과속 3초 지속이면 1건, 계속돼도 중복 없음`() {
        drive(60.0, 5)
        drive(75.0, 60, RoadSegment(limitKmh = 60))
        assertEquals(1, live.violations.size)
        val v = live.violations.single()
        assertEquals(ViolationType.OVERSPEED, v.type)
        assertEquals(15, v.overKmh)
        assertEquals("15km 과속", v.title)
    }

    @Test fun `과속이 끝나고 다시 하면 새 1건`() {
        drive(75.0, 5, RoadSegment(limitKmh = 60))
        drive(60.0, 5)
        drive(75.0, 5, RoadSegment(limitKmh = 60))
        assertEquals(2, live.violations.size)
    }

    @Test fun `급커브 진입 시 40 초과면 1건, 이하면 없음`() {
        val curve = RoadSegment(isCurve = true, locationLabel = "커브")
        drive(55.0, 5)
        drive(55.0, 10, curve)
        assertEquals(1, live.violations.count { it.type == ViolationType.SHARP_CURVE })

        engine.reset()
        drive(30.0, 5)
        drive(40.0, 3, curve) // 정확히 40 → 위반 아님
        drive(60.0, 10, curve) // 이미 진입한 뒤 가속은 판정 X
        assertTrue(live.violations.isEmpty())
    }

    @Test fun `주행 중이 아니면 이벤트 주입 무시`() {
        engine.injectViolation(ViolationType.SAFE_DISTANCE, "x")
        assertTrue(live.violations.isEmpty())
    }

    /** 데모 시나리오(PRD §4): 25km, 7km 급커브, 11km 과속, 13.5km 안전거리 → 23P / 94% */
    @Test fun `데모 시나리오 23P 94퍼센트`() {
        val normal = RoadSegment(limitKmh = 60)
        while (live.distanceKm < 7.0) drive(60.0, 1, normal)
        drive(55.0, 30, RoadSegment(limitKmh = 60, isCurve = true, locationLabel = "종로구 21-22"))
        while (live.distanceKm < 11.0) drive(60.0, 1, normal)
        drive(70.0, 5, RoadSegment(limitKmh = 50, locationLabel = "중구 5-1"))
        while (live.distanceKm < 13.5) drive(60.0, 1, normal)
        engine.injectViolation(ViolationType.SAFE_DISTANCE, "용산구 3-4")
        while (live.distanceKm < 25.0) drive(60.0, 1, normal)
        drive(0.0, 1)
        park()

        assertEquals(DriveState.ENDED, live.state)
        assertEquals(listOf("급커브 구간 미감속", "20km 과속", "안전거리 미확보"), live.violations.map { it.title })
        assertEquals(23, live.livePoints)
        assertEquals(94, live.safeRatio)
        assertEquals("좋아요!", praise(live.safeRatio))
    }
}
