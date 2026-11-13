package com.pure.chageumtong.data.navi

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import com.pure.chageumtong.domain.Place

/**
 * Pleos NaviHelper SDK 3.0.5 래퍼: 사용처 길안내 (PRD §6.3).
 * 흐름(공식 문서): NaviHelper(ctx) → initialize(onConnected, onFailed) → addListener → requestRoute → removeListener → release
 * 결과는 onRoutingCompleted/onRouteStarted(성공) 또는 onError(실패)로 온다. 응답이 없으면 타임아웃으로 실패 처리.
 */
class NaviRoute(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var helper: NaviHelper? = null
    @Volatile private var connected = false
    private var pending: ((Boolean) -> Unit)? = null

    private val listener = object : NaviHelperEventListener {
        override fun onRoutingCompleted() = finish(true)
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) = finish(true)
        override fun onError(errorCode: NaviErrorCode) {
            Log.w(TAG, "navi error $errorCode")
            finish(false)
        }
    }

    fun start() {
        if (helper != null) return
        helper = runCatching {
            NaviHelper(context).apply {
                initialize(
                    onConnected = { connected = true; addListener(listener) },
                    onFailed = { e -> connected = false; Log.w(TAG, "navi connect failed", e) },
                )
            }
        }.onFailure { Log.w(TAG, "navi init failed", it) }.getOrNull()
    }

    fun stop() {
        helper?.runCatching { removeListener(listener); release() }
        helper = null
        connected = false
    }

    /** 목적지로 경로 요청. onResult(성공 여부)는 메인 스레드에서 한 번만 불린다. */
    fun route(place: Place, onResult: (Boolean) -> Unit) {
        val h = helper
        if (h == null || !connected) return onResult(false)
        main.removeCallbacksAndMessages(null)
        pending = onResult
        runCatching {
            h.requestRoute(
                RouteInfo(
                    latitude = place.lat,
                    longitude = place.lon,
                    displayLongitude = null,
                    displayLatitude = null,
                    poiName = place.name,
                    poiId = place.id,
                    poiSubId = "0",
                    address = place.name,
                    routeOption = RouteOption.RECOMMENDED,
                ),
            )
        }.onFailure { Log.w(TAG, "requestRoute failed", it); finish(false); return }
        main.postDelayed({ finish(false) }, TIMEOUT_MS)
    }

    private fun finish(ok: Boolean) {
        main.post {
            val cb = pending ?: return@post
            pending = null
            main.removeCallbacksAndMessages(null)
            cb(ok)
        }
    }

    private companion object {
        const val TAG = "NaviRoute"
        const val TIMEOUT_MS = 8_000L
    }
}
