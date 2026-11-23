package com.kero.forgot

import android.app.Application
import com.kero.forgot.di.AppContainer
import com.kero.forgot.notifications.NotificationHelper
import com.kero.forgot.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ForgotApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
        applicationScope.launch {
            container.seedDataInitializer.seedIfEmpty()
            container.reminderScheduler.syncAll()
            WidgetUpdater.updateAllWidgets(this@ForgotApplication)
        }
    }
}
