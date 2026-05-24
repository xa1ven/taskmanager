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
import com.example.todolist.R
import com.example.todolist.ToDoApplication.Companion.NOTIFICATION_CHANNEL_ID
import com.example.todolist.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class MorningNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val result = taskRepository.getTasks()
        val tasks = result.getOrNull() ?: return Result.success()
        val today = LocalDate.now().toString()
        val todayTasks = tasks.filter { !it.isDone && it.deadline?.startsWith(today) == true }

        if (todayTasks.isEmpty()) return Result.success()

        val highTask = todayTasks.firstOrNull { it.priority == "HIGH" }
        val text = buildString {
            append("У вас ${todayTasks.size} задач на сегодня")
            if (highTask != null) append("\n⚠️ Срочно: ${highTask.title}")
        }

        showNotification(
            id = 1001,
            title = "Задачи на сегодня",
            text = text
        )
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
