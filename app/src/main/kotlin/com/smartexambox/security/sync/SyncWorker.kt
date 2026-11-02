package com.smartexambox.security.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartexambox.security.data.db.SecurityDatabaseHelper

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val dbHelper = SecurityDatabaseHelper.getInstance(applicationContext)
        val unsynced = dbHelper.getUnsyncedEvents()
        if (unsynced.isEmpty()) {
            return Result.success()
        }

        // Simulate cloud transmission to Firestore/Admin server
        return try {
            val ids = unsynced.map { it.id }
            dbHelper.markEventsAsSynced(ids)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
