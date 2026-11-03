package com.example.railone

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal const val APP_LOCK_PREFS = "rail_one_lock"
internal const val DEFAULT_LOCK_AFTER_SECONDS = 60
internal val LOCK_AFTER_OPTIONS = listOf(0 to "Immediately", 60 to "1 minute", 300 to "5 minutes")
private const val LEGACY_PIN_ALGORITHM = "PBKDF2WithHmacSHA1"
private const val PIN_ALGORITHM = "PBKDF2WithHmacSHA256"
internal data class AppLockConfig(val enabled: Boolean = false, val biometric: Boolean = false, val lockAfterSeconds: Int = DEFAULT_LOCK_AFTER_SECONDS)
internal data class PinResult(val accepted: Boolean, val error: String = "")

/** Separate from journey/profile edits and excluded from Android backup. */
internal class AppLockStore(context: Context, userId: String = activeUserId(context)) {
    private val prefs = context.getSharedPreferences(APP_LOCK_PREFS, Context.MODE_PRIVATE)
    private val prefix = if (userId == ORIGINAL_USER_ID) "" else "$userId."
    private fun key(value: String) = prefix + value
    fun config() = AppLockConfig(prefs.getBoolean(key("enabled"), false), prefs.getBoolean(key("biometric"), false),
        prefs.getInt(key("lockAfter"), DEFAULT_LOCK_AFTER_SECONDS).let { saved -> if (LOCK_AFTER_OPTIONS.any { it.first == saved }) saved else DEFAULT_LOCK_AFTER_SECONDS })
    fun save(enabled: Boolean, biometric: Boolean, newPin: String = "", lockAfterSeconds: Int? = null): Boolean {
        if (newPin.isNotEmpty()) require(newPin.matches(Regex("[0-9]{6}")))
        if (enabled && !prefs.contains(key("hash"))) require(newPin.isNotEmpty())
        val edit = prefs.edit().putBoolean(key("enabled"), enabled).putBoolean(key("biometric"), enabled && biometric)
        if (lockAfterSeconds != null && LOCK_AFTER_OPTIONS.any { it.first == lockAfterSeconds }) edit.putInt(key("lockAfter"), lockAfterSeconds)
        if (newPin.isNotEmpty()) {
            putPinHash(edit, newPin)
            edit.putInt(key("failures"), 0).putLong(key("lockedUntil"), 0)
        }
        return edit.commit()
    }
    fun verify(pin: String, now: Long = System.currentTimeMillis()): PinResult {
        if (!pin.matches(Regex("[0-9]{6}"))) return PinResult(false, "Enter your six-digit mPIN.")
        val until = prefs.getLong(key("lockedUntil"), 0)
        if (now < until) return PinResult(false, "Too many attempts. Try again in ${((until - now + 999) / 1000)} seconds.")
        val matches = runCatching {
            val salt = Base64.decode(requireNotNull(prefs.getString(key("salt"), null)), Base64.NO_WRAP)
            val expected = Base64.decode(requireNotNull(prefs.getString(key("hash"), null)), Base64.NO_WRAP)
            MessageDigest.isEqual(expected, derive(pin, salt, prefs.getString(key("alg"), LEGACY_PIN_ALGORITHM) ?: LEGACY_PIN_ALGORITHM))
        }.getOrDefault(false)
        val failures = if (matches) 0 else (if (until != 0L) 0 else prefs.getInt(key("failures"), 0)) + 1
        val update = prefs.edit().putInt(key("failures"), failures).putLong(key("lockedUntil"), if (failures >= 5) now + 30_000 else 0)
        // A correct PIN stored with the older SHA-1 hash is silently upgraded to SHA-256.
        if (matches && prefs.getString(key("alg"), LEGACY_PIN_ALGORITHM) != preferredAlgorithm()) runCatching { putPinHash(update, pin) }
        if (!update.commit())
            return PinResult(false, "Could not save login state. Please retry.")
        return if (matches) PinResult(true) else PinResult(false,
            if (failures >= 5) "Too many attempts. Try again in 30 seconds." else "Incorrect mPIN.")
    }
    private fun preferredAlgorithm(): String =
        // PBKDF2 with SHA-256 needs API 26+. API 24-25 keep SHA-1. Never store the PIN itself.
        runCatching { SecretKeyFactory.getInstance(PIN_ALGORITHM); PIN_ALGORITHM }.getOrDefault(LEGACY_PIN_ALGORITHM)
    private fun putPinHash(edit: android.content.SharedPreferences.Editor, pin: String) {
        val algorithm = preferredAlgorithm()
        val salt = ByteArray(24).also { SecureRandom().nextBytes(it) }
        edit.putString(key("salt"), Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(key("hash"), Base64.encodeToString(derive(pin, salt, algorithm), Base64.NO_WRAP))
            .putString(key("alg"), algorithm)
    }
    private fun derive(pin: String, salt: ByteArray, algorithm: String): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 150_000, 256)
        return try { SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
