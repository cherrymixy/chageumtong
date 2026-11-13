package com.pure.chageumtong.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pure.chageumtong.core.AppContainer
import com.pure.chageumtong.BuildConfig
import com.pure.chageumtong.data.repo.AppData
import com.pure.chageumtong.data.repo.MonthStats
import com.pure.chageumtong.data.repo.PurchaseResult
import com.pure.chageumtong.domain.DriveState
import com.pure.chageumtong.domain.Gear
import com.pure.chageumtong.domain.LiveDrive
import com.pure.chageumtong.domain.Place
import com.pure.chageumtong.domain.Product
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 모든 화면이 공유하는 상태·동작. 입력원이 실차인지 데모인지는 여기서도 모른다. */
class AppViewModel(private val c: AppContainer) : ViewModel() {
    val live: StateFlow<LiveDrive> = c.engine.live
    val demoRunning: StateFlow<Boolean> = c.demo.running
    val zone = c.zone
    val products = c.catalog.products
    /** shortcut: 지도 중심 = 시연용 고정 좌표(서울시청). scripts/fetch_kakao_places.py 의 CENTER와 같아야 함. 실제 GPS를 붙이면 검색 서버가 필요 */
    val mapCenter = 37.5665 to 126.9780
    /** 카카오맵이 있으면 미리 받은 실제 사용처, 없으면(정적 지도 그림) 그림 좌표가 있는 시드 장소 */
    val places: List<Place> = if (BuildConfig.KAKAO_NATIVE_KEY.isNotBlank()) c.catalog.kakaoPlaces else c.catalog.places
    val demoRoute = c.demoRoute

    val data: StateFlow<AppData> = c.points.data.stateIn(viewModelScope, SharingStarted.Eagerly, AppData())
    val month: StateFlow<MonthStats?> = c.points.monthStats.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val week: StateFlow<List<Double>> = c.points.weekDailyKm.stateIn(viewModelScope, SharingStarted.Eagerly, List(7) { 0.0 })
    val favorites: StateFlow<Set<String>> = c.places.favoriteIds.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** PRD §7: 기어≠P 또는 속도>0이면 주행 중. 기어를 모를 때(UNKNOWN)는 속도로만 판단. */
    val isDriving: StateFlow<Boolean> = combine(c.lastInput, c.engine.live) { input, live ->
        val s = input?.sample
        live.state == DriveState.DRIVING ||
            (s != null && ((s.gear != Gear.P && s.gear != Gear.UNKNOWN) || s.speedMps > 0.1))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun startDemo() = c.demo.start()

    /** 0.3km 미만이라 적립할 수 없는 주행 닫기 */
    fun dismissDrive() = c.resetDrive()

    fun claim(onClaimed: (sessionId: String) -> Unit) {
        viewModelScope.launch { c.claimAndReset()?.let { onClaimed(it.id) } }
    }

    /** 지도 [길안내 시작] → NaviHelper. onResult(성공 여부) */
    fun navigate(place: Place, onResult: (Boolean) -> Unit) = c.navi.route(place, onResult)

    /** 분석 화면 진입(새 적립) 시 1회 음성 안내 */
    fun announcePoints(points: Int) = c.tts.announcePoints(points)

    fun purchase(product: Product, onResult: (PurchaseResult) -> Unit) {
        viewModelScope.launch { onResult(c.points.purchase(product)) }
    }

    fun toggleFavorite(placeId: String) {
        viewModelScope.launch { c.places.toggleFavorite(placeId) }
    }

    fun resetAll() {
        viewModelScope.launch { c.points.resetAll() }
    }
}
