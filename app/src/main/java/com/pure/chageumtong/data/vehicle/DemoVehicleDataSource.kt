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
 * 데모 주행: 25km 경로를 가상 시간 1초 = 실제 60ms로 압축 재생 (약 90초, PRD §12 D3).
 * 샘플 시각은 가상 시간이라 엔진에는 약 25분 주행으로 기록된다.
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
        val t0 = System.currentTimeMillis()
        var simSec = 0L
        var km = 0.0
        val events = route.events.sortedBy { it.atKm }.toMutableList()
        fun at(sec: Long) = t0 + sec * 1000

        while (km < route.totalKm) {
            val seg = route.segmentAt(km)
            val mps = seg.speedKmh / 3.6
            val event = events.firstOrNull()?.takeIf { km >= it.atKm }?.also { events.removeAt(0) }
            _inputs.emit(
                DriveInput(VehicleSample(at(simSec), mps, Gear.D), seg.toRoadSegment(), event?.type, event?.label ?: ""),
            )
            km += mps / 1000.0 // 가상 1초 동안 이동
            simSec++
            delay(TICK_MS)
        }
        // 정차 → P (주행 종료 조건)
        _inputs.emit(DriveInput(VehicleSample(at(simSec), 0.0, Gear.D)))
        delay(TICK_MS)
        _inputs.emit(DriveInput(VehicleSample(at(simSec + 1), 0.0, Gear.P)))
    }

    companion object {
        /** 실제 60ms = 가상 1초 → 1500초(25분) 경로가 약 90초 */
        const val TICK_MS = 60L
    }
}
