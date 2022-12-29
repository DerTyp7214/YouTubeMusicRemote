package de.dertyp7214.youtubemusicremote.services

import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.lifecycle.MutableLiveData

class NotificationService : NotificationListenerService() {
    companion object {
        val notifications: MutableLiveData<List<StatusBarNotification>> = MutableLiveData()
    }

    override fun onBind(intent: Intent?): IBinder? {
        notifications.postValue(activeNotifications.toList())
        Log.d("NotificationService", "onBind")

        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        notifications.postValue(activeNotifications.toList())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        notifications.postValue(activeNotifications.toList())
    }
}