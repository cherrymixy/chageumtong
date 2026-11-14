package com.pure.chageumtong.data

import com.pure.chageumtong.data.seed.DemoRoute
import com.pure.chageumtong.data.vehicle.demoInputs
import com.pure.chageumtong.domain.DriveSessionEngine
import com.pure.chageumtong.domain.DriveState
import com.pure.chageumtong.domain.unsafeKm
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** 실제 assets/demo_route.json 한 바퀴를 엔진에 넣었을 때의 결과 (README 예상 결과와 같아야 함) */
class DemoRouteTest {
    @Test fun `데모 한 바퀴 결과`() {
        val route = Json.decodeFromString<DemoRoute>(File("src/main/assets/demo_route.json").readText())
        val engine = DriveSessionEngine()
        demoInputs(route, 0L).forEach { input ->
            engine.onSample(input.sample, input.segment)
            input.inject?.let { engine.injectViolation(it, input.injectLabel) }
        }
        val live = engine.live.value
        println("거리 ${live.distanceKm}km / 미적립 ${unsafeKm(live.violations, live.distanceKm)}km / ${live.livePoints}P / ${live.safeRatio}%")
        live.violations.forEach { println("  ${it.title} at=${it.atKm} [${it.segmentStartKm}, ${it.segmentEndKm}] 등급${it.grade} 유예=${it.waived}") }

        assertEquals(DriveState.ENDED, live.state)
        assertEquals(listOf("급커브 구간 미감속", "20km 과속", "급감속", "안전거리 미확보", "급정지"), live.violations.map { it.title })
        assertEquals(listOf(false, false, true, false, false), live.violations.map { it.waived })
        assertEquals(10.04, live.distanceKm, 0.01)
        assertEquals(0.289, unsafeKm(live.violations, live.distanceKm), 0.001)
        assertEquals(9, live.livePoints)
        assertEquals(97, live.safeRatio)
    }
}
