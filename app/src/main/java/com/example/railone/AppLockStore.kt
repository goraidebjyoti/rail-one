package com.example.railone

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal const val APP_LOCK_PREFS = "rail_one_lock"
internal data class AppLockConfig(val enabled: Boolean = false, val biometric: Boolean = false)
internal data class PinResult(val accepted: Boolean, val error: String = "")

/** Separate from journey/profile edits and excluded from Android backup. */
internal class AppLockStore(context: Context) {
    private val prefs = context.getSharedPreferences(APP_LOCK_PREFS, Context.MODE_PRIVATE)
    fun config() = AppLockConfig(prefs.getBoolean("enabled", false), prefs.getBoolean("biometric", false))
    fun save(enabled: Boolean, biometric: Boolean, newPin: String = ""): Boolean {
        if (newPin.isNotEmpty()) require(newPin.matches(Regex("[0-9]{6}")))
        if (enabled && !prefs.contains("hash")) require(newPin.isNotEmpty())
        val edit = prefs.edit().putBoolean("enabled", enabled).putBoolean("biometric", enabled && biometric)
        if (newPin.isNotEmpty()) {
            val salt = ByteArray(24).also { SecureRandom().nextBytes(it) }
            edit.putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("hash", Base64.encodeToString(derive(newPin, salt), Base64.NO_WRAP))
                .putInt("failures", 0).putLong("lockedUntil", 0)
        }
        return edit.commit()
    }
    fun verify(pin: String, now: Long = System.currentTimeMillis()): PinResult {
        if (!pin.matches(Regex("[0-9]{6}"))) return PinResult(false, "Enter your six-digit mPIN.")
        val until = prefs.getLong("lockedUntil", 0)
        if (now < until) return PinResult(false, "Too many attempts. Try again in ${((until - now + 999) / 1000)} seconds.")
        val matches = runCatching {
            val salt = Base64.decode(requireNotNull(prefs.getString("salt", null)), Base64.NO_WRAP)
            val expected = Base64.decode(requireNotNull(prefs.getString("hash", null)), Base64.NO_WRAP)
            MessageDigest.isEqual(expected, derive(pin, salt))
        }.getOrDefault(false)
        val failures = if (matches) 0 else (if (until != 0L) 0 else prefs.getInt("failures", 0)) + 1
        if (!prefs.edit().putInt("failures", failures).putLong("lockedUntil", if (failures >= 5) now + 30_000 else 0).commit())
            return PinResult(false, "Could not save login state. Please retry.")
        return if (matches) PinResult(true) else PinResult(false,
            if (failures >= 5) "Too many attempts. Try again in 30 seconds." else "Incorrect mPIN.")
    }
    private fun derive(pin: String, salt: ByteArray): ByteArray {
        // Available on every supported Android version (API24+). Never store the PIN itself.
        val spec = PBEKeySpec(pin.toCharArray(), salt, 150_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
