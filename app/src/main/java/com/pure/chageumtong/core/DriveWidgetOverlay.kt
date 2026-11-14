package com.pure.chageumtong.core

import com.pure.chageumtong.domain.DriveSessionEngine
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.pure.chageumtong.MainActivity
import com.pure.chageumtong.domain.DriveState
import com.pure.chageumtong.ui.components.DriveWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 주행 중 다른 앱(내비 등) 위에 뜨는 포인트 위젯 (Figma 105:2337).
 * 앱이 화면에 없고 + 주행 중이고 + '다른 앱 위에 표시' 권한이 있을 때만. 끌어서 옮기고, 누르면 앱으로.
 * shortcut: 앱 프로세스가 살아 있는 동안만 동작 (포그라운드 서비스 없음). 실차 배포 때 서비스로 옮길 것
 */
class DriveWidgetOverlay(private val context: Context, private val container: AppContainer, scope: CoroutineScope) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null

    init {
        scope.launch(Dispatchers.Main) {
            combine(container.engine.live, container.appVisible) { live, visible -> live.state == DriveState.DRIVING && !visible }
                .distinctUntilChanged()
                .collect { if (it) show() else hide() }
        }
    }

    private fun show() {
        if (view != null) return
        if (!Settings.canDrawOverlays(context)) {
            Log.i("DriveWidget", "overlay permission not granted, skip")
            return
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (40 * context.resources.displayMetrics.density).toInt()
        }
        val o = OverlayOwner().also { owner = it }
        view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setContent {
                val live by container.engine.live.collectAsState()
                val input by container.lastInput.collectAsState()
                DriveWidget(
                    live.livePoints,
                    moving = (input?.sample?.speedMps ?: 0.0) > DriveSessionEngine.STOP_MPS,
                    modifier =
                    Modifier
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                params.x += drag.x.toInt()
                                params.y -= drag.y.toInt() // BOTTOM 기준이라 위로 끌면 y 증가
                                view?.let { wm.updateViewLayout(it, params) }
                            }
                        }
                        .clickable { openApp() },
                )
            }
        }
        runCatching { wm.addView(view, params) }.onFailure {
            Log.w("DriveWidget", "addView failed", it)
            view = null
        }
    }

    private fun hide() {
        view?.let { runCatching { wm.removeView(it) } }
        owner?.destroy()
        view = null
        owner = null
    }

    private fun openApp() = context.startActivity(
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
    )

    /** 액티비티 밖 ComposeView에 필요한 생명주기 */
    private class OverlayOwner : SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry get() = saved.savedStateRegistry

        init {
            saved.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() { registry.currentState = Lifecycle.State.DESTROYED }
    }
}
