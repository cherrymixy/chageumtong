package com.pure.chageumtong.ui

import com.pure.chageumtong.data.vehicle.DriveInput
import com.pure.chageumtong.domain.Gear
import com.pure.chageumtong.domain.RoadSegment
import com.pure.chageumtong.domain.VehicleSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveMotionTest {
    private fun s(ms: Long, kmh: Double) = VehicleSample(ms, kmh / 3.6, Gear.D)

    @Test fun `1초에 20kmh 감속은 급정거`() {
        val d = HardBrakeDetector()
        assertFalse(d.onSample(s(0, 60.0)))
        assertTrue(d.onSample(s(1_000, 40.0))) // 5.6 m/s²
    }

    @Test fun `천천히 서면 급정거 아님`() {
        val d = HardBrakeDetector()
        d.onSample(s(0, 60.0))
        assertFalse(d.onSample(s(1_000, 55.0))) // 1.4 m/s²
    }

    @Test fun `창보다 짧은 간격은 모았다가 판단`() {
        val d = HardBrakeDetector()
        d.onSample(s(0, 60.0))
        assertFalse(d.onSample(s(100, 50.0)))
        assertTrue(d.onSample(s(600, 30.0))) // 0→600ms, 30kmh 감속 = 13.9 m/s²
    }

    @Test fun `시간이 되돌아가면 새로 시작`() {
        val d = HardBrakeDetector()
        d.onSample(s(10_000, 60.0))
        assertFalse(d.onSample(s(0, 0.0)))
    }

    @Test fun `움직임과 과속`() {
        assertEquals(DriveMotion(), null.toMotion())
        assertEquals(DriveMotion(moving = false), DriveInput(s(0, 0.0)).toMotion())
        assertEquals(DriveMotion(moving = true), DriveInput(s(0, 70.0), RoadSegment(limitKmh = 60)).toMotion())
        assertEquals(DriveMotion(moving = true, overspeed = true), DriveInput(s(0, 71.0), RoadSegment(limitKmh = 60)).toMotion())
    }
}
