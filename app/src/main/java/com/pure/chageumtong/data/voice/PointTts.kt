package com.pure.chageumtong.data.voice

import android.content.Context
import android.util.Log
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import java.util.Locale

/**
 * Pleos TextToSpeech SDK 3.3.1 래퍼: 적립 완료 음성 안내 (PRD §7).
 * ON_DEVICE 모드 = 앱 등록(Client ID)·네트워크 불필요 (공식 문서 app-registration).
 * 실패·미지원이면 조용히 생략한다.
 */
class PointTts(private val context: Context) {
    private var tts: TextToSpeech? = null
    @Volatile private var ready = false

    private val events = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() = Unit
        override fun onDone() = Unit
        override fun onStop() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onError(errorType: ErrorType) { Log.w(TAG, "tts error $errorType") }
    }

    fun start() {
        if (tts != null) return
        tts = runCatching {
            TextToSpeech(context, Mode.ON_DEVICE).apply {
                initialize(object : OnInitializeListener {
                    override fun onSuccess() {
                        runCatching {
                            addEventListener(events)
                            setLocale(Locale.KOREA)
                            ready = true
                        }.onFailure { Log.w(TAG, "tts setup failed", it) }
                    }
                    override fun onFailure(errorType: ErrorType) { Log.w(TAG, "tts init failed $errorType") }
                })
            }
        }.onFailure { Log.w(TAG, "tts create failed", it) }.getOrNull()
    }

    fun stop() {
        tts?.runCatching { stop(); removeEventListener(events); release() }
        tts = null
        ready = false
    }

    fun announcePoints(points: Int) {
        if (points <= 0) return // 0.3~1km 주행은 0P → "0포인트 적립" 안내는 어색해서 생략
        val t = tts ?: return
        if (!ready) {
            Log.i(TAG, "tts not ready, skip")
            return
        }
        runCatching { t.speak("이번 주행으로 ${points}포인트가 적립됐어요", true) }
            .onFailure { Log.w(TAG, "speak failed", it) }
    }

    private companion object { const val TAG = "PointTts" }
}
