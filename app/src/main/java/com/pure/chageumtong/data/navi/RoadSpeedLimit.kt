package com.pure.chageumtong.data.navi

import android.content.Context
import android.os.SystemClock
import android.util.Log
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

/**
 * 현재 도로 제한속도 (NaviHelper 3.0.5 `NaviHelperEventListener.onCurrentRoadSpeedLimit(Int)`, SDK 클래스에서 확인).
 * NaviRoute는 길안내 요청마다 연결·해제하므로, 주행 동안 연결을 유지하는 별도 객체로 둔다.
 */
class RoadSpeedLimit(private val context: Context) {
    private var helper: NaviHelper? = null
    @Volatile private var limitKmh = 0
    @Volatile private var updatedAt = 0L

    private val listener = object : NaviHelperEventListener {
        override fun onCurrentRoadSpeedLimit(speedLimit: Int) {
            Log.d(TAG, "speed limit $speedLimit")
            limitKmh = speedLimit
            updatedAt = SystemClock.elapsedRealtime()
        }
    }

    fun start() {
        if (helper != null) return
        helper = runCatching {
            NaviHelper(context).apply {
                initialize(
                    onConnected = { addListener(listener) },
                    onFailed = { e -> Log.w(TAG, "navi connect failed", e) },
                )
            }
        }.onFailure { Log.w(TAG, "navi init failed", it) }.getOrNull()
    }

    fun stop() {
        helper?.runCatching { removeListener(listener); release() }
        helper = null
        limitKmh = 0
    }

    /** 최신 제한속도. 값이 없거나 0 이하거나 10초 넘게 갱신이 없으면 null (→ 기본 60) */
    fun current(): Int? =
        limitKmh.takeIf { it > 0 && SystemClock.elapsedRealtime() - updatedAt <= STALE_MS }

    private companion object {
        const val TAG = "RoadSpeedLimit"
        const val STALE_MS = 10_000L
    }
}
