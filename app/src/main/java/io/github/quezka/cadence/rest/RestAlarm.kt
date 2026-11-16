package io.github.quezka.cadence.rest

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.quezka.cadence.CadenceApp
import io.github.quezka.cadence.MainActivity
import io.github.quezka.cadence.R

/** The rest timer outside the app: a countdown in the notification shade while you rest, and a buzz when it's over,
 *  even with the screen off or another app open (an alarm, not a loop inside the screen). */
object RestAlarm {
    private const val NOTE = 1
    private const val ACTION = "io.github.quezka.cadence.REST_OVER"

    private fun pending(context: Context) = PendingIntent.getBroadcast(context, 0,
        Intent(context, RestReceiver::class.java).setAction(ACTION), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun open(context: Context) = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)

    /** Rest until [endMillis]: countdown in the shade, buzz at the end. Called again when the time changes. */
    fun start(context: Context, endMillis: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val exact = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
        if (exact) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMillis, pending(context))
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMillis, pending(context))
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTE, NotificationCompat.Builder(context, CadenceApp.REST_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle(context.getString(R.string.rest))
                .setWhen(endMillis).setUsesChronometer(true).setChronometerCountDown(true).setShowWhen(true)
                .setOngoing(true).setSilent(true).setOnlyAlertOnce(true).setContentIntent(open(context))
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH).build())
        }  // no permission: the buzz still comes
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context))
        NotificationManagerCompat.from(context).cancel(NOTE)
    }

    /** Rest is over: buzz, and say so. */
    fun over(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context))
        val vibrator = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTE, NotificationCompat.Builder(context, CadenceApp.REST_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle(context.getString(R.string.rest_over))
                .setContentText(context.getString(R.string.rest_over_text)).setContentIntent(open(context)).setAutoCancel(true)
                .setTimeoutAfter(5 * 60_000L).setPriority(NotificationCompat.PRIORITY_HIGH).build())
        }
    }
}

class RestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = RestAlarm.over(context)
}
