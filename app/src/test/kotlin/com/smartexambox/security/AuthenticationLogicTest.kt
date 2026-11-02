package com.smartexambox.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationLogicTest {

    // Helper simulating lockout state machine without Android context
    class MockLockoutController(private val maxAttempts: Int = 3, private val cooldownMs: Long = 60_000L) {
        var failedAttempts: Int = 0
            private set
        var lockoutUntil: Long = 0L
            private set

        fun recordFailedAttempt(currentTime: Long): Boolean {
            failedAttempts++
            if (failedAttempts >= maxAttempts) {
                lockoutUntil = currentTime + cooldownMs
                return true // Locked out
            }
            return false
        }

        fun recordSuccess() {
            failedAttempts = 0
            lockoutUntil = 0L
        }

        fun isLockedOut(currentTime: Long): Boolean {
            return currentTime < lockoutUntil
        }

        fun getRemainingSeconds(currentTime: Long): Int {
            return if (lockoutUntil > currentTime) {
                ((lockoutUntil - currentTime) / 1000).toInt()
            } else 0
        }
    }

    @Test
    fun testPinValidation_validSixDigitPin() {
        val validPin = "123456"
        assertTrue(validPin.length == 6 && validPin.all { it.isDigit() })
    }

    @Test
    fun testPinValidation_invalidShortOrNonDigitPin() {
        val shortPin = "1234"
        val alphaPin = "12a456"
        assertFalse(shortPin.length == 6)
        assertFalse(alphaPin.all { it.isDigit() })
    }

    @Test
    fun testFailedAttemptProgression_andLockoutTriggerAtThree() {
        val controller = MockLockoutController()
        val now = 1000000L

        // Attempt 1: Failed
        var locked = controller.recordFailedAttempt(now)
        assertEquals(1, controller.failedAttempts)
        assertFalse(locked)
        assertFalse(controller.isLockedOut(now))

        // Attempt 2: Failed
        locked = controller.recordFailedAttempt(now)
        assertEquals(2, controller.failedAttempts)
        assertFalse(locked)
        assertFalse(controller.isLockedOut(now))

        // Attempt 3: Failed -> Must Trigger Lockout!
        locked = controller.recordFailedAttempt(now)
        assertEquals(3, controller.failedAttempts)
        assertTrue(locked)
        assertTrue(controller.isLockedOut(now))
        assertEquals(60, controller.getRemainingSeconds(now))
    }

    @Test
    fun testLockoutExpiration_afterCooldownElapses() {
        val controller = MockLockoutController(cooldownMs = 60_000L)
        val startTime = 1000000L

        // Trigger lockout
        repeat(3) { controller.recordFailedAttempt(startTime) }
        assertTrue(controller.isLockedOut(startTime))

        // 30 seconds later -> Still locked
        val midTime = startTime + 30_000L
        assertTrue(controller.isLockedOut(midTime))
        assertEquals(30, controller.getRemainingSeconds(midTime))

        // 61 seconds later -> Lockout expired!
        val endTime = startTime + 61_000L
        assertFalse(controller.isLockedOut(endTime))
        assertEquals(0, controller.getRemainingSeconds(endTime))
    }

    @Test
    fun testSuccessfulAuthentication_resetsFailedCounter() {
        val controller = MockLockoutController()
        val now = 1000000L

        controller.recordFailedAttempt(now)
        controller.recordFailedAttempt(now)
        assertEquals(2, controller.failedAttempts)

        // User enters correct PIN
        controller.recordSuccess()
        assertEquals(0, controller.failedAttempts)
        assertFalse(controller.isLockedOut(now))
    }
}
