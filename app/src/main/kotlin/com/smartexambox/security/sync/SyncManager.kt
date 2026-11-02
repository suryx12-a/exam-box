package com.smartexambox.security.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.smartexambox.security.data.db.SecurityDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class SyncStatusState(
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val pendingSyncCount: Int = 0,
    val lastSyncResult: String = "Ready"
)

class SyncManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)
    private val dbHelper = SecurityDatabaseHelper.getInstance(context)

    private val _syncState = MutableStateFlow(
        SyncStatusState(pendingSyncCount = dbHelper.getPendingSyncCount())
    )
    val syncState: StateFlow<SyncStatusState> = _syncState.asStateFlow()

    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    suspend fun performImmediateSync(isNetworkAvailable: Boolean): Boolean = withContext(Dispatchers.IO) {
        val pendingCount = dbHelper.getPendingSyncCount()
        if (!isNetworkAvailable) {
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                pendingSyncCount = pendingCount,
                lastSyncResult = "Offline: Events queued locally in Room database"
            )
            return@withContext false
        }

        if (pendingCount == 0) {
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncResult = "All security events up to date (0 pending)"
            )
            return@withContext true
        }

        _syncState.value = _syncState.value.copy(isSyncing = true, lastSyncResult = "Synchronizing $pendingCount events...")

        // Simulate cloud payload delivery
        kotlinx.coroutines.delay(1200)

        val unsynced = dbHelper.getUnsyncedEvents()
        val ids = unsynced.map { it.id }
        val syncedCount = dbHelper.markEventsAsSynced(ids)

        _syncState.value = SyncStatusState(
            isSyncing = false,
            lastSyncTimestamp = System.currentTimeMillis(),
            pendingSyncCount = dbHelper.getPendingSyncCount(),
            lastSyncResult = "Successfully synchronized $syncedCount event(s) to cloud server"
        )
        true
    }

    fun updatePendingCount() {
        _syncState.value = _syncState.value.copy(
            pendingSyncCount = dbHelper.getPendingSyncCount()
        )
    }

    companion object {
        const val WORK_NAME_PERIODIC_SYNC = "SmartExamPeriodicSync"

        @Volatile
        private var instance: SyncManager? = null

        fun getInstance(context: Context): SyncManager {
            return instance ?: synchronized(this) {
                instance ?: SyncManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
