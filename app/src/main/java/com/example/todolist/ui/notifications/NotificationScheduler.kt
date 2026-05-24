package com.example.todolist.ui.notifications

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val MORNING_WORK_TAG = "morning_notification"
    private const val EVENING_WORK_TAG = "evening_notification"

    fun scheduleMorningNotification(context: Context) {
        val delay = calculateDelayUntilHour(8)
        val request = PeriodicWorkRequestBuilder<MorningNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(MORNING_WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MORNING_WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleEveningNotification(context: Context) {
        val delay = calculateDelayUntilHour(20)
        val request = PeriodicWorkRequestBuilder<EveningNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(EVENING_WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            EVENING_WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelMorningNotification(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(MORNING_WORK_TAG)
    }

    fun cancelEveningNotification(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(EVENING_WORK_TAG)
    }

    private fun calculateDelayUntilHour(hour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}
