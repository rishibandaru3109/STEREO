param(
    [ValidatePattern('^[A-Za-z0-9_-]{11}$')][string]$TrackId = 'AQ5NlI-SJR0',
    [string]$Serial = '75YTAYG6U4VKZ5OZ',
    [switch]$TestOffline
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
$projectRoot = Split-Path -Parent $PSScriptRoot
$apk = Join-Path $projectRoot 'scratch/artifacts/STEREO-personal-cache-debug.apk'
$results = Join-Path $projectRoot ('scratch/device-cache-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $results -Force | Out-Null
$runId = [guid]::NewGuid().ToString()
$package = 'com.nova.music.debug'
$component = "$package/com.nova.music.testing.CacheVerificationActivity"

function Invoke-Adb {
    # Plain function keeps native -W/-n/--es switches out of PowerShell common-parameter binding.
    $output = & $adb -s $Serial @args 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($output -join [Environment]::NewLine) }
    return $output
}
function Invoke-Phase {
    param([string]$Phase, [int]$TimeoutSeconds = 90, [switch]$Background)
    Write-Host "Testing $Phase..."
    Invoke-Adb shell am force-stop $package | Out-Null
    Invoke-Adb shell am start -W -n $component --es phase $Phase --es trackId $TrackId --es runId $runId | Out-Null
    if ($Background) {
        Start-Sleep -Seconds 3
        Invoke-Adb shell input keyevent KEYCODE_HOME | Out-Null
    }
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        # The result lives in this debuggable app's private directory; run-as avoids root.
        $json = & $adb -s $Serial exec-out run-as $package cat "files/cache-verification/$Phase.json" 2>$null
        if ($LASTEXITCODE -eq 0) {
            try { $result = ($json -join "`n") | ConvertFrom-Json } catch { $result = $null }
            if ($null -ne $result -and $result.runId -eq $runId) {
                $result | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $results "$Phase.json") -Encoding utf8
                $result | Format-List | Out-Host
                if (-not $result.passed) { throw "Device verification failed at $Phase. See $results" }
                return $result
            }
        }
        Start-Sleep -Seconds 2
    }
    throw "Timed out during $Phase. See device screen and logs."
}

try {
    Invoke-Adb get-state | Out-Null
    Invoke-Adb shell svc power stayon true | Out-Null
    Invoke-Adb shell logcat -c | Out-Null
    Invoke-Adb install -r $apk | Out-Host
    # These tests clear automatic audio cache, leave playlists/likes/history intact, and play one chosen song.
    $allResults = @()
    $allResults += Invoke-Phase fixture
    $allResults += Invoke-Phase first -TimeoutSeconds 360
    $allResults += Invoke-Phase replay
    $allResults += Invoke-Phase restart
    $allResults += Invoke-Phase seek
    $allResults += Invoke-Phase background -Background
    if ($TestOffline) {
        $wifiBefore = (Invoke-Adb shell settings get global wifi_on | Out-String).Trim()
        $dataBefore = (Invoke-Adb shell settings get global mobile_data | Out-String).Trim()
        if ($wifiBefore -notin @('0', '1') -or $dataBefore -notin @('0', '1')) {
            throw 'Cannot safely snapshot radio settings; offline phase was not run.'
        }
        try {
            Invoke-Adb shell svc wifi disable | Out-Null
            Invoke-Adb shell svc data disable | Out-Null
            $allResults += Invoke-Phase offline
        } finally {
            if ($wifiBefore -eq '1') { Invoke-Adb shell svc wifi enable | Out-Null }
            if ($dataBefore -eq '1') { Invoke-Adb shell svc data enable | Out-Null }
        }
    }
    $allResults | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $results 'summary.json') -Encoding utf8
    Write-Host "Device results saved to $results"
} finally {
    Invoke-Adb shell svc power stayon false 2>$null | Out-Null
    & $adb -s $Serial logcat -d -v threadtime -s STEREO_CACHE:V STEREO_CACHE_TEST:V NOVA_DIAGNOSTICS:I AndroidRuntime:E '*:S' |
        Set-Content (Join-Path $results 'device.log') -Encoding utf8
}
