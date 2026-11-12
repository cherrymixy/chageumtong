package com.pure.chageumtong.core

import android.content.Context
import com.pure.chageumtong.data.seed.DemoRouteLoader
import com.pure.chageumtong.data.vehicle.DemoVehicleDataSource
import com.pure.chageumtong.data.vehicle.DriveInput
import com.pure.chageumtong.data.vehicle.PleosVehicleDataSource
import com.pure.chageumtong.domain.DriveSessionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 수동 DI. 앱 전체에서 하나씩만 존재하는 객체들. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val engine = DriveSessionEngine()
    val pleos = PleosVehicleDataSource(appContext)
    val demoRoute by lazy { DemoRouteLoader.load(appContext) }
    val demo = DemoVehicleDataSource(scope) { demoRoute }

    private val _lastInput = MutableStateFlow<DriveInput?>(null)
    /** 엔진이 마지막으로 받은 입력 (디버그 표시용) */
    val lastInput: StateFlow<DriveInput?> = _lastInput.asStateFlow()

    init {
        // 데모 재생 중이면 실차 입력은 무시 (데모 우선)
        scope.launch { pleos.inputs.collect { if (!demo.running.value) feed(it) } }
        scope.launch { demo.inputs.collect { feed(it) } }
    }

    /** 적립 처리 후 대기 상태로 (STEP 4에서 저장 로직이 앞에 붙는다) */
    fun resetDrive() = synchronized(engine) { engine.reset() }

    private fun feed(input: DriveInput) = synchronized(engine) {
        engine.onSample(input.sample, input.segment)
        input.inject?.let { engine.injectViolation(it, input.injectLabel) }
        _lastInput.value = input
    }
}
