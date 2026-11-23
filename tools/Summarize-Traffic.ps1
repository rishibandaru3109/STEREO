param(
    [Parameter(Mandatory=$true)][string]$LogPath,
    [Parameter(Mandatory=$true)][string]$Scenario,
    [string]$OutputPath = 'scratch/traffic-measurement.json'
)
$ErrorActionPreference = 'Stop'
$pattern = '(?<time>\d+\.\d+)\s+(?<pid>\d+)\s+\d+\s+\w\s+STEREO_TRAFFIC.*audio=(?<audio>\d+) artwork=(?<artwork>\d+) api=(?<api>\d+) requests=(?<requests>\d+) declaredUpload=(?<upload>\d+) metadataHits=(?<metadataHits>\d+) artworkHits=(?<artworkHits>\d+)'
$samples = @(foreach ($line in Get-Content -LiteralPath $LogPath) {
    if ($line -match $pattern) {
        @{
            pid = [long]$Matches.pid
            time = [double]::Parse($Matches.time, [Globalization.CultureInfo]::InvariantCulture)
            audio = [long]$Matches.audio
            artwork = [long]$Matches.artwork
            api = [long]$Matches.api
            requests = [long]$Matches.requests
            upload = [long]$Matches.upload
            metadataHits = [long]$Matches.metadataHits
            artworkHits = [long]$Matches.artworkHits
        }
    }
})
if ($samples.Count -lt 2) { throw 'Capture at least two Sample traffic readings using adb logcat -v epoch.' }
$before = $samples[-2]
$after = $samples[-1]
if ($after.pid -ne $before.pid) { throw 'App process changed between snapshots. Repeat with one process.' }
$elapsed = $after.time - $before.time
if ($elapsed -le 0) { throw 'Sample timestamps must be increasing.' }
$delta = @{}
foreach ($kind in @('audio','artwork','api','requests','upload','metadataHits','artworkHits')) {
    $delta[$kind] = $after[$kind] - $before[$kind]
    if ($delta[$kind] -lt 0) { throw 'Counters reset between samples. Repeat without restarting the app.' }
}
$result = [ordered]@{
    scenario = $Scenario
    evidence = 'Device HTTP payload counters; excludes transport overhead'
    durationSeconds = [math]::Round($elapsed, 3)
    audioBytes = $delta.audio
    artworkBytes = $delta.artwork
    apiBytes = $delta.api
    totalResponseBytes = $delta.audio + $delta.artwork + $delta.api
    networkRequests = $delta.requests
    declaredUploadBytes = $delta.upload
    metadataCacheHits = $delta.metadataHits
    artworkCacheHits = $delta.artworkHits
    audioDiskCache = 'Disabled for YouTube sources'
}
$json = $result | ConvertTo-Json
$json | Set-Content -LiteralPath $OutputPath -Encoding utf8
$json
