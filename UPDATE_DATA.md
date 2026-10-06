# Updating without losing demo data

Future builds keep the same application ID, development signing certificate and preferences format. Install the new APK over the old one; do not uninstall. GitHub builds increase the version code automatically. Routes are added to existing storage; tickets, passengers, templates, wallet and profile stay intact.

## First transition from an older APK

Older GitHub builds used a temporary debug key. If Android refuses an update because the signatures differ, that private key cannot be recovered from an installed APK. This transition needs a backup before replacing the old installation.

On Windows, install Android SDK platform-tools, enable USB debugging, connect the phone, approve its USB debugging prompt, and check `adb devices`. Connect only one device. The old demo must still be installed and debuggable. Keep the APK and the whole project including the signing folder.

From the project folder in PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\Update-RailOne.ps1 -Apk "C:\Downloads\app-debug.apk"
```

This first saves the app's preferences and private files to a `.tar` backup plus a checksum `.json`, then tries an in-place update. If a signing mismatch occurs, it stops with the backup intact. Copy both files somewhere safe. Only then, to explicitly allow a one-time replacement, rerun with a new backup filename:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\Update-RailOne.ps1 -Apk "C:\Downloads\app-debug.apk" -AllowSigningMigration
```

This creates another backup, tries the update again, and only for the signature mismatch uninstalls, installs the new APK and restores data. Open the app and verify tickets, passengers and profile before discarding any backups. Preferences include local login settings; private files include the profile photograph. The script does not send your data anywhere.

For recovery after a failed replacement, install the new APK and restore your existing backup:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\Update-RailOne.ps1 -Mode Restore -BackupFile "C:\path\rail-one-data-YYYYMMDD-HHMMSS.tar"
```

Restoring overwrites matching saved files. It does not merge newer data. If `run-as` or backup fails, the tool stops before uninstalling. Do not uninstall manually. This tool cannot recover data already removed by uninstalling.

## Saved routes

Use **You → Saved Routes → Add Route**, or **Booking Details → Save Route and Reverse**. From, To and optional Via are stored independently of people. Via stops use hyphens: `PKU-SRC` reverses to `SRC-PKU`. Leave Via empty for direct journeys. Different Via paths for the same stations are separate saved choices. Editing updates both directions; deleting a route pair removes both directions but leaves tickets and other route variants intact.

Choose **Use Saved Route** in Booking Details to fill From, To and Via. All remain editable. Passenger name and phone continue to come from your You profile. Existing journey templates are retained, and their route details migrate automatically into this separate list.
