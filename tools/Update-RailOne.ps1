[CmdletBinding()]
param(
    [ValidateSet('Update','Backup','Restore')][string]$Mode = 'Update',
    [string]$Apk,
    [string]$BackupFile = (Join-Path $PWD ("rail-one-data-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.tar')),
    [string]$Adb = 'adb',
    [switch]$AllowSigningMigration
)
$ErrorActionPreference = 'Stop'
$package = 'com.example.railone'
function Run-Adb([string[]]$Arguments) {
    $result = & $Adb @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($result -join "`n") }
    return ($result -join "`n")
}
# Transfer tar bytes directly. PowerShell 5 text redirection corrupts binary archives.
function Transfer([string]$Arguments, [string]$Path, [bool]$Restore) {
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $Adb
    $info.Arguments = $Arguments
    $info.UseShellExecute = $false
    $info.RedirectStandardError = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardInput = $Restore
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $info
    [void]$process.Start()
    $errors = $process.StandardError.ReadToEndAsync()
    try {
        if ($Restore) {
            $output = $process.StandardOutput.ReadToEndAsync()
            $stream = [System.IO.File]::OpenRead($Path)
            try { $stream.CopyTo($process.StandardInput.BaseStream) } finally { $stream.Dispose(); $process.StandardInput.Close() }
        } else {
            $stream = [System.IO.File]::Create($Path)
            try { $process.StandardOutput.BaseStream.CopyTo($stream) } finally { $stream.Dispose() }
        }
        $process.WaitForExit()
        $errorText = $errors.GetAwaiter().GetResult()
        if ($process.ExitCode -ne 0) { throw "ADB archive transfer failed: $errorText" }
    } finally { $process.Dispose() }
}
function Save-Backup {
    if (Test-Path $BackupFile) { throw "Backup already exists; choose a new -BackupFile path." }
    [void](Run-Adb @('shell','am','force-stop',$package))
    [void](Run-Adb @('shell','run-as',$package,'test','-d','shared_prefs'))
    $folders = 'shared_prefs'
    & $Adb shell run-as $package test -d files 2>$null
    if ($LASTEXITCODE -eq 0) { $folders += ' files' }
    try { Transfer "exec-out run-as $package tar -cf - $folders" $BackupFile $false }
    catch { Remove-Item $BackupFile -ErrorAction SilentlyContinue; throw }
    if ((Get-Item $BackupFile).Length -lt 512) { throw 'Backup is empty. No replacement attempted.' }
    @{ package=$package; sha256=(Get-FileHash $BackupFile -Algorithm SHA256).Hash; folders=$folders; created=(Get-Date).ToString('o') } |
        ConvertTo-Json | Set-Content -Encoding UTF8 "$BackupFile.json"
    Write-Host "Verified data backup saved: $BackupFile"
}
function Restore-Backup {
    $metadata = Get-Content -Raw "$BackupFile.json" | ConvertFrom-Json
    if ($metadata.package -ne $package -or $metadata.sha256 -ne (Get-FileHash $BackupFile -Algorithm SHA256).Hash) {
        throw 'Backup identity or checksum mismatch. Restore cancelled.'
    }
    [void](Run-Adb @('shell','am','force-stop',$package))
    Transfer "shell -T run-as $package tar -xf -" $BackupFile $true
    Write-Host 'Data restored. Open Rail One and check tickets, passengers and profile.'
}
[void](Run-Adb @('get-state'))
if ($Mode -eq 'Restore') { Restore-Backup; exit }
if ($Mode -eq 'Backup') { Save-Backup; exit }
if (-not $Apk -or -not (Test-Path $Apk -PathType Leaf)) { throw 'Provide -Apk pointing to the updated APK.' }
Save-Backup
$result = & $Adb install -r $Apk 2>&1
$installExit = $LASTEXITCODE
if ($installExit -eq 0) { Write-Host 'Updated in place; existing app data retained.'; exit }
if (($result -join "`n") -notmatch 'INSTALL_FAILED_UPDATE_INCOMPATIBLE') { throw ($result -join "`n") }
if (-not $AllowSigningMigration) {
    throw "Old signing key differs. Backup is safe. To perform the ONE-TIME replacement, rerun with -AllowSigningMigration and a NEW backup filename. Do not uninstall manually."
}
Write-Host 'Performing explicitly requested one-time signing-key migration using the verified backup.'
[void](Run-Adb @('uninstall',$package))
try {
    [void](Run-Adb @('install',$Apk))
    Restore-Backup
} catch { throw "Replacement/restore failed. KEEP $BackupFile and $BackupFile.json. Install the new APK then run -Mode Restore -BackupFile with this archive. Details: $_" }
