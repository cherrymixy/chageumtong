package com.pure.chageumtong

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.pure.chageumtong.core.ChageumtongApp
import com.pure.chageumtong.data.vehicle.PleosVehicleDataSource
import com.pure.chageumtong.ui.AppRoot
import com.pure.chageumtong.ui.theme.ChageumtongTheme

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
                    AppRoot(container, Modifier.padding(innerPadding))
                }
            }
        }
        container.pleos.start()
        container.navi.start()
        container.tts.start()
        if (checkSelfPermission(PleosVehicleDataSource.CAR_SPEED) != PackageManager.PERMISSION_GRANTED) {
            requestSpeedPermission.launch(PleosVehicleDataSource.CAR_SPEED)
        }
    }

    override fun onStart() {
        super.onStart()
        container.appVisible.value = true
    }

    override fun onStop() {
        container.appVisible.value = false
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) {
            container.pleos.stop()
            container.navi.stop()
            container.tts.stop()
        }
        super.onDestroy()
    }
}
