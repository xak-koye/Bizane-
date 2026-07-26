package com.bizane.app.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

/**
 * بەڕێوەبردنی ئاگادارکردنەوەی ناوخۆیی (local notifications) بۆ بەسەرچوونی خواردن.
 * هاوشێوەی NotificationManager.swift ـە: هیچ سێرڤەرێکی پێویست نییە.
 *
 * لە ئایفۆنەکە ئاگادارکردنەوە بۆ هەر ئایتمێک بەشێوەی تاک تاک ڕیکدەخرێت (notifyEnabled/notifyDaysBefore
 * لەسەر ئایتمەکەی خۆی). لێرە بۆ سادەیی و گونجاندن لەگەڵ AppSettings.notifDays ـی ئەندرۆید (ڕێکخستنێکی
 * گشتی بۆ هەموو ئایتمەکان)، ڕۆژانە یەک پشکنین دەکرێت (کاتژمێر ٩ی بەیانی) و ئاگادارکردنەوە بۆ هەموو
 * ئایتمە خواردنە بۆ بەسەرچوونیان لە ماوەی AppSettings.notifDays ـدا دەردەکەون.
 */
object ExpiryNotifications {
    private const val CHANNEL_ID = "food_expiry"
    private const val REQUEST_CODE = 9001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            L("notif.title"),
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.createNotificationChannel(channel)
    }

    /** داوای ڕێگە دەکات بۆ ناردنی ئاگادارکردنەوە لە ئەندرۆید 13+ (POST_NOTIFICATIONS) */
    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** ڕۆژانە لە کاتژمێر ٩ی بەیانی پشکنین دەکات — ئەگەر کاتژمێر ٩ پێشتر تێپەڕیوە، لە بەیانی سبەینێ دەستپێدەکات */
    fun scheduleDailyCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ExpiryCheckReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pending
            )
        } catch (e: SecurityException) {
            // ئەگەر ڕێگە نەدرا، بەبێ کارەساتی ئەپ تێدەپەڕین — ئاگادارکردنەوە کارا نابێت
        }
    }

    /** پشکنینی خواردنەکان و ناردنی ئاگادارکردنەوە بۆ ئەوانەی لە ماوەی AppSettings.notifDays ـدا بەسەردەچن */
    fun checkAndNotify(context: Context) {
        if (!hasPermission(context)) return
        val notifDays = AppSettings.notifDays
        val items = FoodStorage.items.filter { !it.isExpired && it.daysLeft <= notifDays }
        if (items.isEmpty()) return

        val builder = { item: FoodItem ->
            val body = if (item.daysLeft == 0) L("notif.bodyToday", item.name)
                       else L("notif.bodyDays", item.name, item.daysLeft)
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(L("notif.title"))
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        }

        val nmCompat = NotificationManagerCompat.from(context)
        items.forEach { item ->
            try {
                nmCompat.notify(item.id.hashCode(), builder(item))
            } catch (e: SecurityException) { /* ڕێگە نەدراوە */ }
        }
    }
}

/** BroadcastReceiver ـی ڕۆژانە + دوای ڕیبووت دووبارە ڕیکدەخات */
class ExpiryCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ExpiryNotifications.scheduleDailyCheck(context)
            return
        }
        ExpiryNotifications.checkAndNotify(context)
    }
}
