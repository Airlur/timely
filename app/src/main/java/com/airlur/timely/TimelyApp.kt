package com.airlur.timely

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.room.Room
import com.airlur.timely.data.AppDatabase

class TimelyApp : Application() {

    val db: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "timely.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDER,
                getString(R.string.notification),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                enableLights(true)
            }
        )
    }

    companion object {
        const val CHANNEL_REMINDER = "reminder"
    }
}
