package com.pure.chageumtong

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import com.pure.chageumtong.ui.theme.ChageumtongTheme
import kotlin.math.roundToInt

private const val TAG = "CashDriving"
private const val CAR_SPEED = "android.car.permission.CAR_SPEED"

class MainActivity : ComponentActivity() {

    private var gear by mutableStateOf("-")
    private var speedKmh by mutableStateOf<Int?>(null)

    // 기어: Pleos Vehicle SDK
    private lateinit var vehicle: Vehicle
    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) = runOnUiThread { this@MainActivity.gear = gear.label() }
        override fun onFailed(e: Exception) { Log.e(TAG, "gear listener failed", e) }
    }

    // 차속: AAOS Car API (Vehicle SDK 2.3.2에 차속 API 없음)
    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private val speedCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val mps = value.value as? Float ?: return
            runOnUiThread { speedKmh = (mps * 3.6f).roundToInt() } // m/s → km/h
        }
        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.e(TAG, "speed error: property=$propertyId area=$areaId")
        }
    }

    private val requestSpeedPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startSpeed() else Log.w(TAG, "CAR_SPEED denied")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChageumtongTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(Modifier.padding(innerPadding), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("차금통", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                        DriveStatus(gear, speedKmh, Modifier.weight(1f))
                    }
                }
            }
        }
        startGear()
        if (checkSelfPermission(CAR_SPEED) == PackageManager.PERMISSION_GRANTED) startSpeed()
        else requestSpeedPermission.launch(CAR_SPEED)
    }

    private fun startGear() {
        vehicle = Vehicle(this).apply { initialize() }
        val gearApi = vehicle.getGear()
        gearApi.getCurrentGearSelectionState(
            { value -> runOnUiThread { gear = value.label() } },
            { e -> Log.e(TAG, "get gear failed", e) },
        )
        gearApi.registerGearSelection(gearListener)
    }

    private fun startSpeed() {
        car = Car.createCar(this)
        carPropertyManager = (car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager)?.also {
            it.registerCallback(speedCallback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_UI)
        }
    }

    override fun onDestroy() {
        vehicle.getGear().unregisterGearSelection(gearListener)
        vehicle.release()
        carPropertyManager?.unregisterCallback(speedCallback)
        car?.disconnect()
        super.onDestroy()
    }
}

private fun VehicleGear?.label(): String = when (this) {
    VehicleGear.GEAR_PARK -> "P"
    VehicleGear.GEAR_REVERSE -> "R"
    VehicleGear.GEAR_NEUTRAL -> "N"
    VehicleGear.GEAR_DRIVE -> "D"
    VehicleGear.GEAR_UNKNOWN, null -> "-"
    else -> name.removePrefix("GEAR_") // 수동 1~9단
}

@Composable
fun DriveStatus(gear: String, speedKmh: Int?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("기어", fontSize = 32.sp)
            Text(gear, fontSize = 160.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("속도 (km/h)", fontSize = 32.sp)
            Text(speedKmh?.toString() ?: "--", fontSize = 160.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
fun DriveStatusPreview() {
    ChageumtongTheme { DriveStatus("D", 62) }
}
