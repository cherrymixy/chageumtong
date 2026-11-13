package com.pure.chageumtong.core

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk
import com.pure.chageumtong.BuildConfig

class ChageumtongApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.KAKAO_NATIVE_KEY.isNotBlank()) KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_KEY)
        container = AppContainer(this)
    }
}
