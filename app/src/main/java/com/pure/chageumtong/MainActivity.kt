package com.pure.chageumtong

import android.Manifest
import android.content.Intent
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
import com.pure.chageumtong.core.DriveNotifier
import com.pure.chageumtong.data.vehicle.PleosVehicleDataSource
import com.pure.chageumtong.ui.AppRoot
import com.pure.chageumtong.ui.theme.ChageumtongTheme

class MainActivity : ComponentActivity() {

    private val container get() = (application as ChageumtongApp).container

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result[PleosVehicleDataSource.CAR_SPEED] == true) container.pleos.start() else Log.w("CashDriving", "CAR_SPEED denied")
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
        // 차속 + 주행 알림(Android 13+ 런타임 권한)
        val missing = listOf(PleosVehicleDataSource.CAR_SPEED, Manifest.permission.POST_NOTIFICATIONS)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions.launch(missing.toTypedArray())
        handleAction(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAction(intent)
    }

    private fun handleAction(intent: Intent?) {
        intent?.action?.takeIf { it == DriveNotifier.ACTION_CLAIM }
            ?.let {
                container.notifier.dismiss() // 알림 버튼으로 들어오면 앱 팝업도 닫음
                container.pendingAction.value = it
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
