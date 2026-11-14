package com.pure.chageumtong.data.vehicle

import com.pure.chageumtong.data.seed.DemoRoute
import com.pure.chageumtong.domain.Gear
import com.pure.chageumtong.domain.VehicleSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 데모 주행: 5km 경로를 가상 시간 1초 = 실제 60ms로 압축 재생 (약 20초, PRD §12 D3).
 * 샘플 시각은 가상 시간이라 엔진에는 약 10분 주행으로 기록된다.
 */
class DemoVehicleDataSource(
    private val scope: CoroutineScope,
    private val route: () -> DemoRoute,
) : VehicleDataSource {

    private val _inputs = MutableSharedFlow<DriveInput>(extraBufferCapacity = 64)
    override val inputs: Flow<DriveInput> = _inputs.asSharedFlow()
    override val isAvailable: StateFlow<Boolean> = MutableStateFlow(true)

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private var job: Job? = null

    override fun start() {
        if (job?.isActive == true) return
        _running.value = true
        job = scope.launch {
            try {
                play(route())
            } finally {
                _running.value = false
            }
        }
    }

    override fun stop() {
        job?.cancel()
    }

    private suspend fun play(route: DemoRoute) {
        demoInputs(route, System.currentTimeMillis()).forEach {
            _inputs.emit(it)
            delay(TICK_MS)
        }
    }

    companion object {
        /** 실제 60ms = 가상 1초 → 약 330초(5분 반) 경로가 약 20초 */
        const val TICK_MS = 60L
    }
}

/**
 * 데모 경로 → 가상 1초 간격 입력 목록. 0에서 출발해 구간 속도까지 rampKmhPerS로 서서히 바꾸고,
 * 끝나면 stopKmhPerS로 정지 → P. 구간 사이 계단식 속도 변화가 급가속·급감속으로 오판되지 않게 한다.
 */
fun demoInputs(route: DemoRoute, t0: Long): List<DriveInput> {
    val out = mutableListOf<DriveInput>()
    var simSec = 0L
    var km = 0.0
    var kmh = 0.0
    val events = route.events.sortedBy { it.atKm }.toMutableList()
    fun sample(gear: Gear = Gear.D) = VehicleSample(t0 + simSec * 1000, kmh / 3.6, gear)

    out += DriveInput(sample()) // 정지 상태에서 출발 (급출발 판정용 기준 샘플)
    simSec++
    while (km < route.totalKm) {
        val seg = route.segmentAt(km)
        kmh = approach(kmh, seg.speedKmh, seg.rampKmhPerS)
        val event = events.firstOrNull()?.takeIf { km >= it.atKm }?.also { events.removeAt(0) }
        out += DriveInput(sample(), seg.toRoadSegment(), event?.type, event?.label ?: "")
        km += kmh / 3600.0 // 가상 1초 동안 이동
        simSec++
    }
    // 정차 → P (주행 종료 조건)
    val last = route.segments.last().toRoadSegment()
    while (kmh > 0.0) {
        kmh = approach(kmh, 0.0, route.stopKmhPerS)
        out += DriveInput(sample(), last)
        simSec++
    }
    out += DriveInput(sample(Gear.P))
    return out
}

private fun approach(cur: Double, target: Double, step: Double): Double =
    if (cur < target) minOf(cur + step, target) else maxOf(cur - step, target)
