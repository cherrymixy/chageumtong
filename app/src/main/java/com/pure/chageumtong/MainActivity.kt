package com.pure.chageumtong

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
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pure.chageumtong.core.AppContainer
import com.pure.chageumtong.core.ChageumtongApp
import com.pure.chageumtong.data.vehicle.PleosVehicleDataSource
import com.pure.chageumtong.domain.DriveState
import com.pure.chageumtong.domain.praise
import com.pure.chageumtong.ui.theme.ChageumtongTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val container get() = (application as ChageumtongApp).container

    private val requestSpeedPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) container.pleos.start() else Log.w("CashDriving", "CAR_SPEED denied")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChageumtongTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DebugScreen(container, Modifier.padding(innerPadding))
                }
            }
        }
        container.pleos.start()
        if (checkSelfPermission(PleosVehicleDataSource.CAR_SPEED) != PackageManager.PERMISSION_GRANTED) {
            requestSpeedPermission.launch(PleosVehicleDataSource.CAR_SPEED)
        }
    }

    override fun onDestroy() {
        if (isFinishing) container.pleos.stop()
        super.onDestroy()
    }
}

/** shortcut: STEP 3 임시 디버그 화면, STEP 5 회색박스 화면으로 교체 */
@Composable
private fun DebugScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val live by container.engine.live.collectAsState()
    val demoRunning by container.demo.running.collectAsState()
    val pleosOk by container.pleos.isAvailable.collectAsState()
    val input by container.lastInput.collectAsState()
    val big = 28.sp

    Column(modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("차금통 · 디버그", fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text("입력원: ${if (demoRunning) "데모" else "Pleos"}  (Pleos 사용 가능: $pleosOk)", fontSize = big)
        Text(
            "기어 ${input?.sample?.gear ?: "-"} · 속도 ${input?.sample?.let { (it.speedMps * 3.6).roundToInt() } ?: "-"} km/h · 제한 ${input?.segment?.limitKmh ?: "-"}",
            fontSize = big,
        )
        Text("상태 ${live.state}", fontSize = big, fontWeight = FontWeight.Bold)
        Text(
            "거리 ${"%.2f".format(live.distanceKm)} km · 시간 ${live.durationSec / 60}분 ${live.durationSec % 60}초",
            fontSize = big,
        )
        Text("적립 예정 ${live.livePoints}P · 안전 ${live.safeRatio}% ${praise(live.safeRatio)}", fontSize = big)
        live.violations.forEach {
            Text("· ${it.title}  @${"%.2f".format(it.atKm)}km  ${it.locationLabel}", fontSize = big)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { container.demo.start() }, enabled = !demoRunning && live.state != DriveState.DRIVING) {
                Text("▶ 데모 주행", fontSize = big)
            }
            Button(onClick = { container.resetDrive() }, enabled = live.state == DriveState.ENDED) {
                Text(if (live.canClaim) "적립 처리 (초기화)" else "종료 (0.3km 미만, 적립 불가)", fontSize = big)
            }
        }
    }
}
