package com.alitaalab.medicalfollow

import android.app.Application
import com.alitaalab.medicalfollow.ai.ApiKeyStore
import com.alitaalab.medicalfollow.data.AppDatabase
import com.alitaalab.medicalfollow.data.Repository
import com.alitaalab.medicalfollow.reminders.ReminderWorker
import com.alitaalab.medicalfollow.reminders.Notifications

class MedicalApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: Repository
        private set
    lateinit var apiKeyStore: ApiKeyStore
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.create(this)
        repository = Repository(database)
        apiKeyStore = ApiKeyStore(this)
        Notifications.createChannel(this)
        ReminderWorker.schedule(this)
    }
}
