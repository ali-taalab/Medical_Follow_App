package com.alitaalab.medicalfollow.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.alitaalab.medicalfollow.R
import com.alitaalab.medicalfollow.MedicalApp
import com.alitaalab.medicalfollow.util.formatted
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Runs once a day and notifies about vaccines due within the next 3 days (or overdue) and
 * doctor visits today or tomorrow.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = (applicationContext as MedicalApp).database
        run {
            val today = LocalDate.now()
            val ctx = applicationContext

            val vaccines = db.vaccineDao().pendingUntil(today.plusDays(3))
            if (vaccines.isNotEmpty()) {
                val lines = vaccines.joinToString("\n") { v ->
                    val whenText = when {
                        v.dueDate.isBefore(today) -> ctx.getString(R.string.status_overdue)
                        v.dueDate == today -> ctx.getString(R.string.today)
                        else -> v.dueDate.formatted()
                    }
                    "• ${v.name} — $whenText"
                }
                Notifications.show(ctx, 1001, ctx.getString(R.string.notif_vaccines_title), lines)
            }

            val visits = db.visitDao().visitsBetween(today, today.plusDays(1))
            if (visits.isNotEmpty()) {
                val lines = visits.joinToString("\n") { v ->
                    val day = if (v.date == today) ctx.getString(R.string.today) else ctx.getString(R.string.tomorrow)
                    val who = listOf(v.doctorName, v.specialty).filter { it.isNotBlank() }.joinToString(" - ")
                    "• $day ${v.time} ${who.ifBlank { v.reason }}".replace("  ", " ")
                }
                Notifications.show(ctx, 1002, ctx.getString(R.string.notif_visits_title), lines)
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_reminders"

        fun schedule(context: Context) {
            // First run at the next 9:00 AM, then every 24 hours.
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(9, 0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next).toMinutes()

            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
