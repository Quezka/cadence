package io.github.quezka.cadence

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import io.github.quezka.cadence.data.GymStore

class CadenceApp : Application() {
    val store by lazy { GymStore(this) }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(REST_CHANNEL, getString(R.string.rest_channel), NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val REST_CHANNEL = "rest"
    }
}

val Context.store: GymStore get() = (applicationContext as CadenceApp).store
