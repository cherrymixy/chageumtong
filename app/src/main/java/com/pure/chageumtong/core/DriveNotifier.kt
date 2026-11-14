package com.pure.chageumtong.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.app.Notification
import com.pure.chageumtong.MainActivity
import com.pure.chageumtong.R
import com.pure.chageumtong.domain.Gear
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 주행 알림 2종 (Figma 97:2250). 지금 띄울 알림을 [alert]로 내보내 앱 안 팝업이 그리고, 같은 내용을 시스템 알림 센터에도 남긴다.
 * 차량용 안드로이드는 전화·길안내·메시지 알림만 화면 위(헤드업)에 띄우므로 (AOSP CarHeadsUpNotificationManager), 팝업은 앱이 직접 그린다.
 * - 주행 끝: 적립 가능한 주행이 끝나고 P로 2초
 * - 재탑승: 적립 안 한 주행이 있는데 정차 상태로 P에서 다른 기어로 (출발하면 새 주행이 시작돼 이전 주행은 적립 못 함)
 * (N모드 광고 알림은 Pleos 가이드 "알림에 광고 금지"로 제거)
 * 조건이 깨지면 알림을 지우고, 같은 주행에선 한 번만 띄운다.
 */
class DriveNotifier(private val context: Context, private val container: AppContainer, scope: CoroutineScope) {
    private val nm = context.getSystemService(NotificationManager::class.java)
    private val shown = mutableSetOf<String>()

    data class Alert(val title: String, val text: String, val actionLabel: String, val action: String)

    private val _alert = MutableStateFlow<Alert?>(null)
    /** 지금 띄울 알림 (앱 안 팝업용). 조건이 깨지거나 닫으면 null */
    val alert: StateFlow<Alert?> = _alert

    private enum class Kind { NONE, END, REBOARD }
    private data class Cond(val kind: Kind, val key: Long?, val points: Int)

    init {
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "주행 포인트 알림", NotificationManager.IMPORTANCE_HIGH),
        )
        scope.launch {
            combine(container.engine.live, container.lastInput) { live, input ->
                val gear = input?.sample?.gear
                val stopped = (input?.sample?.speedMps ?: 0.0) <= 0.1
                val kind = when {
                    live.canClaim && stopped && gear != null && gear != Gear.P && gear != Gear.UNKNOWN -> Kind.REBOARD
                    live.canClaim && gear == Gear.P -> Kind.END
                    else -> Kind.NONE
                }
                Cond(kind, live.startedAtMs, if (kind == Kind.END) live.livePoints else 0) // 주행 중 점수 변화로 매번 갱신되지 않게
            }.distinctUntilChanged().collectLatest { c ->
                dismiss()
                when (c.kind) {
                    Kind.NONE -> Unit
                    Kind.REBOARD -> once("re-${c.key}") {
                        post("잊어버린 포인트가 있어요..", "출발하기 전, 잊지말고 포인트를 적립하세요!", "포인트 적립하기", ACTION_CLAIM)
                    }
                    Kind.END -> {
                        delay(2000)
                        once("end-${c.key}") {
                            post("🚗 ${c.points}P가 쌓였어요!", "빠르게 주행 분석하고 포인트를 적립하세요!", "포인트 적립하기", ACTION_CLAIM)
                        }
                    }
                }
            }
        }
    }

    private inline fun once(tag: String, block: () -> Unit) {
        if (shown.add(tag)) block()
    }

    /** 팝업 [닫기]·알림 [닫기]·조건 깨짐 */
    fun dismiss() {
        _alert.value = null
        nm.cancel(ID)
    }

    private fun post(title: String, text: String, actionLabel: String, action: String) {
        _alert.value = Alert(title, text, actionLabel, action)
        val open = PendingIntent.getActivity(
            context, action.hashCode(),
            Intent(context, MainActivity::class.java).setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val close = PendingIntent.getBroadcast(
            context, 0, Intent(context, NotificationCloseReceiver::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_car)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.app_logo))
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, actionLabel, open).build())
            .addAction(Notification.Action.Builder(null, "닫기", close).build())
            .build()
        runCatching { nm.notify(ID, n) } // 알림 권한이 없으면 알림 센터만 생략 (팝업은 뜸)
    }

    companion object {
        const val CHANNEL = "drive_points"
        const val ID = 1
        const val ACTION_CLAIM = "com.pure.chageumtong.CLAIM"
    }
}

/** 알림의 [닫기] */
class NotificationCloseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as ChageumtongApp).container.notifier.dismiss()
    }
}
