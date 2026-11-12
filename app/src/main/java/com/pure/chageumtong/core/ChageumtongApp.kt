package com.pure.chageumtong.core

import android.app.Application

class ChageumtongApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
