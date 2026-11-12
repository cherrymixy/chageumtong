package com.pure.chageumtong.data.vehicle

import com.pure.chageumtong.domain.RoadSegment
import com.pure.chageumtong.domain.VehicleSample
import com.pure.chageumtong.domain.ViolationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** 엔진에 들어가는 입력 한 건. inject: 외부 감지 이벤트(안전거리 미확보 등, 데모 전용). */
data class DriveInput(
    val sample: VehicleSample,
    val segment: RoadSegment = RoadSegment(),
    val inject: ViolationType? = null,
    val injectLabel: String = "",
)

/** 실차/데모 입력원 공통 규격. 화면과 엔진은 이 뒤에 무엇이 있는지 모른다 (CLAUDE.md 규칙 5). */
interface VehicleDataSource {
    val inputs: Flow<DriveInput>
    val isAvailable: StateFlow<Boolean>
    fun start()
    fun stop()
}
