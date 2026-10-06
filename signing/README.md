# Stable development signing identity

Keep `rail-one-debug.jks` unchanged in every checkout and future update. It is a development-only signing key, intentionally bundled for reproducible demo APK updates. Alias: `androiddebugkey`; passwords: `android`.

The application ID remains `com.example.railone`. GitHub builds use version code `1000000 + GITHUB_RUN_NUMBER`. For local builds after installing a CI APK, supply a higher code, for example `gradle assembleDebug -PrailOneVersionCode=2000000`; increase it for later local updates. Do not change app ID or regenerate this key. Do not use this public development key for a production app.

An older installed APK may have been signed with a different runner-generated key. Android cannot update it using this key. See `UPDATE_DATA.md` for a backup-first, one-time transition. Subsequent APKs signed with this bundled key can update in place without uninstalling.
