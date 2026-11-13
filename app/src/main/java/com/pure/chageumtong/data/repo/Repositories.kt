package com.pure.chageumtong.data.repo

import com.pure.chageumtong.domain.DriveSession
import com.pure.chageumtong.domain.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId

class SessionRepository(private val store: AppStore) {
    /** 최신순 */
    val sessions: Flow<List<DriveSession>> = store.data.map { d -> d.sessions.sortedByDescending { it.startedAt } }

    /** true = 이번 호출로 적립됨. 같은 세션 두 번째 호출이나 0.3km 미만이면 false. */
    suspend fun claimSession(session: DriveSession, now: Long = System.currentTimeMillis()): Boolean {
        var claimed = false
        store.update { d -> d.claim(session, now)?.also { claimed = true } ?: d }
        return claimed
    }
}

class PointRepository(
    private val store: AppStore,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val seed: () -> AppData,
) {
    val data: Flow<AppData> = store.data
    val totalPoints: Flow<Int> = store.data.map { it.totalPoints }
    val monthStats: Flow<MonthStats> = store.data.map { it.monthStats(System.currentTimeMillis(), zone()) }
    val weekDailyKm: Flow<List<Double>> = store.data.map { it.weekDailyKm(System.currentTimeMillis(), zone()) }

    suspend fun purchase(product: Product, now: Long = System.currentTimeMillis()): PurchaseResult {
        var result: PurchaseResult = PurchaseResult.AlreadyOwned
        store.update { d -> d.purchase(product, now).let { (next, r) -> result = r; next } }
        return result
    }

    /** 첫 실행(또는 데이터 파일이 비었을 때)에만 시드 */
    suspend fun seedIfNeeded() {
        store.update { if (it.seeded) it else seed() }
    }

    /** 데이터 초기화 (홈 숨김 메뉴·마이페이지). 음성 안내 설정은 유지 */
    suspend fun resetAll() {
        store.update { seed().copy(ttsOn = it.ttsOn) }
    }

    suspend fun setTts(on: Boolean) {
        store.update { it.copy(ttsOn = on) }
    }
}

class PlaceRepository(private val store: AppStore) {
    val favoriteIds: Flow<Set<String>> = store.data.map { it.favoritePlaceIds }

    suspend fun toggleFavorite(placeId: String) {
        store.update { d ->
            d.copy(favoritePlaceIds = if (placeId in d.favoritePlaceIds) d.favoritePlaceIds - placeId else d.favoritePlaceIds + placeId)
        }
    }
}
