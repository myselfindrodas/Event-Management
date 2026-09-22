package com.example.eventmanagement

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.eventmanagement.util.ThemePrefs
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class EventManagementApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemePrefs.applySavedTheme(this)
        enableFirestoreOfflinePersistence()
        createNotificationChannel()
    }

    private fun enableFirestoreOfflinePersistence() {
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder().build()
            )
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Event Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming events"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "event_reminders"
    }
}
