package com.isora.ciclo.security

/**
 * Lets a feature that opens a system screen (a file picker) keep the app unlocked while it is away.
 * Without it the lock removes the screen that waits for the picker's result and the result is lost.
 * Set [suspended] right before launching and clear it when the result, or the cancellation, arrives.
 */
class LockGuard {
    @Volatile
    var suspended: Boolean = false
}
