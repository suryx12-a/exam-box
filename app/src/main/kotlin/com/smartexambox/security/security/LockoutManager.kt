package com.smartexambox.security.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LockoutManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lockout_prefs", Context.MODE_PRIVATE)

    private val _failedAttemptsFlow = MutableStateFlow(getSavedFailedAttempts())
    val failedAttemptsFlow: StateFlow<Int> = _failedAttemptsFlow.asStateFlow()

    private val _isLockedOutFlow = MutableStateFlow(checkIsLockedOut())
    val isLockedOutFlow: StateFlow<Boolean> = _isLockedOutFlow.asStateFlow()

    fun recordFailedAttempt(): LockoutState {
        val current = getSavedFailedAttempts() + 1
        saveFailedAttempts(current)
        _failedAttemptsFlow.value = current

        if (current >= MAX_FAILED_ATTEMPTS) {
            val lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
            saveLockoutUntil(lockoutUntil)
            _isLockedOutFlow.value = true
            return LockoutState(
                isLockedOut = true,
                failedAttempts = current,
                remainingSeconds = (LOCKOUT_DURATION_MS / 1000).toInt()
            )
        }

        return LockoutState(
            isLockedOut = false,
            failedAttempts = current,
            remainingSeconds = 0
        )
    }

    fun recordSuccessfulAttempt() {
        saveFailedAttempts(0)
        saveLockoutUntil(0L)
        _failedAttemptsFlow.value = 0
        _isLockedOutFlow.value = false
    }

    fun checkIsLockedOut(): Boolean {
        val until = getLockoutUntil()
        val now = System.currentTimeMillis()
        val locked = now < until
        _isLockedOutFlow.value = locked
        if (!locked && getSavedFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            // Lockout expired, reset attempts
            saveFailedAttempts(0)
            _failedAttemptsFlow.value = 0
        }
        return locked
    }

    fun getRemainingLockoutSeconds(): Int {
        val until = getLockoutUntil()
        val now = System.currentTimeMillis()
        return if (until > now) {
            ((until - now) / 1000).toInt()
        } else {
            0
        }
    }

    private fun getSavedFailedAttempts(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    private fun saveFailedAttempts(count: Int) = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, count).apply()

    private fun getLockoutUntil(): Long = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
    private fun saveLockoutUntil(timestamp: Long) = prefs.edit().putLong(KEY_LOCKOUT_UNTIL, timestamp).apply()

    companion object {
        const val MAX_FAILED_ATTEMPTS = 3
        const val LOCKOUT_DURATION_MS = 60_000L // 60 seconds lockout
        private const val KEY_FAILED_ATTEMPTS = "key_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "key_lockout_until"

        @Volatile
        private var instance: LockoutManager? = null

        fun getInstance(context: Context): LockoutManager {
            return instance ?: synchronized(this) {
                instance ?: LockoutManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

data class LockoutState(
    val isLockedOut: Boolean,
    val failedAttempts: Int,
    val remainingSeconds: Int
)
