package com.pure.chageumtong.core

import android.content.Context
import com.pure.chageumtong.data.repo.AppStore
import com.pure.chageumtong.data.repo.PlaceRepository
import com.pure.chageumtong.data.repo.PointRepository
import com.pure.chageumtong.data.repo.SessionRepository
import com.pure.chageumtong.data.seed.CatalogLoader
import com.pure.chageumtong.data.seed.DemoRouteLoader
import com.pure.chageumtong.data.seed.SeedData
import com.pure.chageumtong.data.vehicle.DemoVehicleDataSource
import com.pure.chageumtong.data.vehicle.DriveInput
import com.pure.chageumtong.data.vehicle.PleosVehicleDataSource
import com.pure.chageumtong.domain.DriveSession
import com.pure.chageumtong.domain.DriveSessionEngine
import com.pure.chageumtong.domain.DriveState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId

/** 수동 DI. 앱 전체에서 하나씩만 존재하는 객체들. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val engine = DriveSessionEngine()
    val pleos = PleosVehicleDataSource(appContext)
    val demoRoute by lazy { DemoRouteLoader.load(appContext) }
    val demo = DemoVehicleDataSource(scope) { demoRoute }

    /** 통계·날짜는 항상 한국 시간 기준 (에뮬레이터 기본 시간대가 America/Los_Angeles라서) */
    val zone: ZoneId = ZoneId.of("Asia/Seoul")

    val catalog by lazy { CatalogLoader.load(appContext) }
    private val store = AppStore(appContext, scope)
    val sessions = SessionRepository(store)
    val points = PointRepository(store, zone = { zone }) {
        SeedData.create(System.currentTimeMillis(), zone, catalog.products)
    }
    val places = PlaceRepository(store)

    private val _lastInput = MutableStateFlow<DriveInput?>(null)
    /** 엔진이 마지막으로 받은 입력 (디버그 표시용) */
    val lastInput: StateFlow<DriveInput?> = _lastInput.asStateFlow()

    /** 지금 주행이 데모 경로였으면 그 ID (분석 화면 지도 연결용) */
    private var currentRouteId: String? = null

    init {
        scope.launch { points.seedIfNeeded() }
        // 데모 재생 중이면 실차 입력은 무시 (데모 우선)
        scope.launch { pleos.inputs.collect { if (!demo.running.value) feed(it, fromDemo = false) } }
        scope.launch { demo.inputs.collect { feed(it, fromDemo = true) } }
    }

    /**
     * 정차 후 [주행 분석하고 적립하기]: 세션 저장 + 적립 → 엔진 대기 상태로.
     * 세션 ID = 출발 시각이라 두 번 눌러도 한 번만 적립된다. 반환: 저장된 세션(적립 불가면 null).
     */
    suspend fun claimAndReset(): DriveSession? {
        val live = engine.live.value
        if (live.state != DriveState.ENDED) return null
        val startedAt = live.startedAtMs ?: return null
        val session = DriveSession(
            id = "drive-$startedAt",
            startedAt = startedAt,
            endedAt = live.endedAtMs,
            distanceKm = live.distanceKm,
            durationSec = live.durationSec,
            violations = live.violations,
            routeId = currentRouteId,
            claimed = false,
        )
        val claimed = sessions.claimSession(session)
        resetDrive()
        return if (claimed) session.copy(claimed = true) else null
    }

    fun resetDrive() = synchronized(engine) { engine.reset() }

    private fun feed(input: DriveInput, fromDemo: Boolean) = synchronized(engine) {
        val before = engine.live.value.state
        engine.onSample(input.sample, input.segment)
        if (before != DriveState.DRIVING && engine.live.value.state == DriveState.DRIVING) {
            currentRouteId = if (fromDemo) demoRoute.id else null
        }
        input.inject?.let { engine.injectViolation(it, input.injectLabel) }
        _lastInput.value = input
    }
}
