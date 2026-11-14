package com.pure.chageumtong.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveSessionEngineTest {
    private val engine = DriveSessionEngine()
    private var t = 0L

    /** stepMs 간격 샘플을 count개 보낸다. */
    private fun drive(kmh: Double, count: Int, segment: RoadSegment = RoadSegment(), gear: Gear = Gear.D, stepMs: Long = 1000) {
        repeat(count) {
            engine.onSample(VehicleSample(t, kmh / 3.6, gear), segment)
            t += stepMs
        }
    }

    private fun park() = engine.onSample(VehicleSample(t, 0.0, Gear.P))
    private val live get() = engine.live.value
    private fun of(vararg types: ViolationType) = live.violations.filter { it.type in types }
    private val decels get() = of(ViolationType.HARD_DECEL, ViolationType.HARD_STOP)
    private val overspeeds get() = of(ViolationType.OVERSPEED)

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
        drive(60.0, 5)
        assertTrue(overspeeds.isEmpty())
    }

    @Test fun `10점5 초과 2점9초는 없음, 3초면 1건`() {
        drive(60.0, 20, stepMs = 100)
        drive(70.5, 30, stepMs = 100) // 0~2.9초
        drive(60.0, 50, stepMs = 100)
        assertTrue(overspeeds.isEmpty())

        drive(70.5, 31, stepMs = 100) // 0~3.0초
        drive(60.0, 50, stepMs = 100)
        assertEquals(1, overspeeds.size)
    }

    @Test fun `과속 에피소드는 정상 3초 뒤 마감, 계속돼도 1건`() {
        drive(60.0, 5)
        drive(75.0, 60, RoadSegment(limitKmh = 60))
        assertTrue(overspeeds.isEmpty()) // 아직 진행 중
        drive(60.0, 4)
        val v = overspeeds.single()
        assertEquals(15, v.overKmh)
        assertEquals(1, v.grade)
        assertEquals("15km 과속", v.title)
        // 1등급: 버퍼 없이 과속 시작 ~ 처음 내려온 지점
        assertEquals(v.atKm, v.segmentStartKm, 1e-9)
        assertTrue(v.segmentEndKm - v.segmentStartKm > 1.2) // 75km/h × 60초 ≈ 1.25km
    }

    @Test fun `정상 2초 후 다시 과속하면 한 에피소드`() {
        drive(75.0, 5, RoadSegment(limitKmh = 60))
        drive(60.0, 2)
        drive(75.0, 5, RoadSegment(limitKmh = 60))
        drive(60.0, 5)
        assertEquals(1, overspeeds.size)
    }

    @Test fun `정상 3초 지나고 다시 과속하면 새 1건`() {
        drive(75.0, 5, RoadSegment(limitKmh = 60))
        drive(60.0, 5)
        drive(75.0, 5, RoadSegment(limitKmh = 60))
        drive(60.0, 5)
        assertEquals(2, overspeeds.size)
    }

    @Test fun `과속 중 P로 끝나면 그 지점에서 마감`() {
        drive(60.0, 5)
        drive(75.0, 10, RoadSegment(limitKmh = 60))
        park()
        assertEquals(DriveState.ENDED, live.state)
        assertEquals(live.distanceKm, overspeeds.single().segmentEndKm, 1e-9)
    }

    @Test fun `65 초과 과속은 4등급, 주행 전체 미적립`() {
        drive(60.0, 60)
        drive(125.0, 5, RoadSegment(limitKmh = 60))
        drive(60.0, 5)
        assertTrue(overspeeds.single().voidsTrip)
        assertEquals(0, live.livePoints)
        assertEquals(0, live.safeRatio)
    }

    @Test fun `급감속 2초 간격 2회면 첫 건만 유예`() {
        drive(60.0, 5)
        drive(50.0, 2) // 급감속 1 (유예)
        drive(40.0, 10) // 2초 뒤 급감속 2
        assertEquals(2, decels.size)
        assertTrue(decels[0].waived)
        assertFalse(decels[1].waived)
        assertEquals(1, live.visibleViolations.size)
        // 둘째 건만 차감: 50km/h 정지시거 55m
        assertEquals(0.055, unsafeKm(live.violations, live.distanceKm), 1e-9)
    }

    @Test fun `0점8초 간격 급감속은 1건`() {
        drive(60.0, 10, stepMs = 200)
        drive(50.0, 4, stepMs = 200)
        drive(40.0, 10, stepMs = 200)
        assertEquals(1, decels.size)
    }

    @Test fun `급감속이 이어져 정지하면 1건, 급정지`() {
        drive(60.0, 5)
        listOf(50.0, 40.0, 30.0, 20.0, 10.0, 0.0).forEach { drive(it, 1) }
        val v = decels.single()
        assertEquals(ViolationType.HARD_STOP, v.type)
        assertEquals(60, v.speedKmh)
    }

    @Test fun `0에서 1초에 12km면 급출발, 정지시거 20m`() {
        drive(0.0, 1, stepMs = 500)
        drive(6.0, 1, stepMs = 500) // 주행 시작 샘플은 판정 안 함
        drive(12.0, 1, stepMs = 500)
        val v = of(ViolationType.HARD_START).single()
        assertEquals(0, v.speedKmh)
        assertEquals(0.02, (v.segmentEndKm - v.atKm) * 2, 1e-9)
    }

    @Test fun `1초에 −45km는 센서 튐으로 무시`() {
        drive(60.0, 5)
        drive(15.0, 5)
        assertTrue(decels.isEmpty())
    }

    @Test fun `급가속은 3초 안 반복이면 1건`() {
        drive(20.0, 3)
        listOf(32.0, 44.0, 56.0).forEach { drive(it, 1) }
        assertEquals(1, of(ViolationType.HARD_ACCEL).size)
    }

    @Test fun `급커브 진입 시 40 초과면 1건, 이하면 없음`() {
        val curve = RoadSegment(isCurve = true, locationLabel = "커브")
        drive(55.0, 5)
        drive(55.0, 10, curve)
        val c = of(ViolationType.SHARP_CURVE).single()
        assertEquals(0.055, c.segmentEndKm - c.segmentStartKm, 1e-9) // 55km/h → 50 → 55m

        engine.reset()
        drive(30.0, 5)
        drive(40.0, 3, curve) // 정확히 40 → 위반 아님
        drive(60.0, 10, curve) // 이미 진입한 뒤 가속은 판정 X
        assertTrue(of(ViolationType.SHARP_CURVE).isEmpty())
    }

    @Test fun `주행 중이 아니면 이벤트 주입 무시`() {
        engine.injectViolation(ViolationType.SAFE_DISTANCE, "x")
        assertTrue(live.violations.isEmpty())
    }
}
