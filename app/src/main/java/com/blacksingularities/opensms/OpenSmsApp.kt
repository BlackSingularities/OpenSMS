package com.blacksingularities.opensms

import android.app.Application
import com.blacksingularities.opensms.notify.Notifier

class OpenSmsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppStrings.context = this
        Notifier.createChannels(this)
    }
}
