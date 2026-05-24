package com.example.todolist.ui.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todolist.MainActivity
import com.example.todolist.ToDoApplication.Companion.NOTIFICATION_CHANNEL_ID
import com.example.todolist.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EveningNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val result = taskRepository.getTasks()
        val tasks = result.getOrNull() ?: return Result.success()
        val pending = tasks.filter { !it.isDone }

        val (title, text) = if (pending.isEmpty()) {
            "Итоги дня" to "Отличная работа! Все задачи выполнены 🎉"
        } else {
            "Итоги дня" to "Осталось незавершённых задач: ${pending.size}. Не забудьте закрыть или перенести их."
        }

        showNotification(id = 1002, title = title, text = text)
        return Result.success()
    }

    private fun showNotification(id: Int, title: String, text: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(id, notification)
    }
}
