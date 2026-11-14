package com.pure.chageumtong.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.pure.chageumtong.data.navi.RoadSpeedLimit
import com.pure.chageumtong.domain.DEFAULT_LIMIT_KMH
import com.pure.chageumtong.domain.Gear
import com.pure.chageumtong.domain.RoadSegment
import com.pure.chageumtong.domain.VehicleSample
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * 실차 입력원. 기어 = Pleos Vehicle SDK, 속도 = AAOS CarPropertyManager PERF_VEHICLE_SPEED
 * (Vehicle SDK 2.3.2에 속도 API 없음, PRD §12). 제한속도 = NaviHelper 현재 도로 제한속도, 없으면 60.
 */
class PleosVehicleDataSource(private val context: Context) : VehicleDataSource {

    private var gearOk = false
    private var speedOk = false
    private val _available = MutableStateFlow(false)
    override val isAvailable: StateFlow<Boolean> = _available.asStateFlow()

    @Volatile private var gear = Gear.UNKNOWN
    @Volatile private var speedMps = 0.0

    private var vehicle: Vehicle? = null
    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private val speedLimit = RoadSpeedLimit(context)

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) { this@PleosVehicleDataSource.gear = gear.toGear() }
        override fun onFailed(e: Exception) { Log.e(TAG, "gear listener failed", e) }
    }

    private val speedCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            (value.value as? Float)?.let { speedMps = it.toDouble() }
        }
        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.e(TAG, "speed error: property=$propertyId area=$areaId")
        }
    }

    /** 속도가 일정하면 변경 이벤트가 안 올 수 있어, 최신값을 일정 주기로 샘플링해 거리 적분이 끊기지 않게 한다. */
    override val inputs: Flow<DriveInput> = flow {
        while (true) {
            val segment = RoadSegment(limitKmh = speedLimit.current() ?: DEFAULT_LIMIT_KMH)
            emit(DriveInput(VehicleSample(System.currentTimeMillis(), speedMps, gear), segment))
            delay(SAMPLE_MS)
        }
    }

    /** 여러 번 불러도 안전. 속도는 CAR_SPEED 권한이 있을 때만 시작되므로 권한 허용 후 다시 부른다. */
    override fun start() {
        if (vehicle == null) startGear()
        if (carPropertyManager == null) startSpeed()
        speedLimit.start()
    }

    override fun stop() {
        vehicle?.let {
            it.getGear().unregisterGearSelection(gearListener)
            it.release()
        }
        vehicle = null
        speedLimit.stop()
        carPropertyManager?.unregisterCallback(speedCallback)
        carPropertyManager = null
        car?.disconnect()
        car = null
        gearOk = false
        speedOk = false
        updateAvailable()
    }

    private fun startGear() {
        val v = Vehicle(context).apply { initialize() }
        vehicle = v
        val gearApi = v.getGear()
        // 기어에는 checkXXXCapabilities가 없어 첫 조회 성공 여부로 지원을 판단한다 (PRD §12)
        gearApi.getCurrentGearSelectionState(
            { value -> gear = value.toGear(); gearOk = true; updateAvailable() },
            { e -> Log.e(TAG, "gear not available", e); gearOk = false; updateAvailable() },
        )
        gearApi.registerGearSelection(gearListener)
    }

    private fun startSpeed() {
        if (context.checkSelfPermission(CAR_SPEED) != PackageManager.PERMISSION_GRANTED) return
        val c = Car.createCar(context) ?: return
        car = c
        val cpm = c.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager ?: return
        if (cpm.getCarPropertyConfig(VehiclePropertyIds.PERF_VEHICLE_SPEED) == null) {
            Log.w(TAG, "PERF_VEHICLE_SPEED not supported")
            return
        }
        carPropertyManager = cpm
        speedOk = cpm.registerCallback(speedCallback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_UI)
        updateAvailable()
    }

    private fun updateAvailable() { _available.value = gearOk && speedOk }

    private fun VehicleGear?.toGear(): Gear = when (this) {
        VehicleGear.GEAR_PARK -> Gear.P
        VehicleGear.GEAR_REVERSE -> Gear.R
        VehicleGear.GEAR_NEUTRAL -> Gear.N
        VehicleGear.GEAR_DRIVE -> Gear.D
        else -> Gear.UNKNOWN // 수동 1~9단 등은 이 앱에서 다루지 않음
    }

    companion object {
        const val CAR_SPEED = "android.car.permission.CAR_SPEED"
        private const val TAG = "PleosVehicle"
        private const val SAMPLE_MS = 500L
    }
}
