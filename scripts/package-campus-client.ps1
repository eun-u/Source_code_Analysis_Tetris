param([string]$PublicConfigPath = '')
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$taskOut = Join-Path $taskRoot 'out'
$taskJar = Join-Path $taskOut 'tetris.jar'
if (-not (Test-Path -LiteralPath $taskJar)) { throw 'Build the game before packaging.' }
if (-not $PublicConfigPath) { $PublicConfigPath = Join-Path $taskOut 'tetris-client.properties' }
$publicValues = @{}
foreach ($line in [IO.File]::ReadAllLines($PublicConfigPath)) {
    if ($line -match '^\s*[#!]' -or -not $line.Trim()) { continue }
    if ($line -notmatch '^\s*(server\.url|supabase\.url|supabase\.publishableKey)\s*=\s*(.+)\s*$') {
        throw 'Only three public client configuration fields may be packaged.'
    }
    $publicValues[$Matches[1]] = $Matches[2].Trim()
}
if ($publicValues.Count -ne 3) { throw 'Incomplete public client configuration.' }
$serverUri = [Uri]$publicValues['server.url']
$databaseUri = [Uri]$publicValues['supabase.url']
if ($serverUri.Scheme -ne 'wss' -or $databaseUri.Scheme -ne 'https') { throw 'Public services require TLS.' }
$publicKey = $publicValues['supabase.publishableKey']
if (-not $publicKey.StartsWith('sb_publishable_')) {
    $segments = $publicKey.Split('.')
    if ($segments.Count -ne 3) { throw 'Only a publishable or anon key is allowed.' }
    $payload = $segments[1].Replace('-', '+').Replace('_', '/')
    $payload += '=' * ((4 - $payload.Length % 4) % 4)
    $claims = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload)) | ConvertFrom-Json
    if ($claims.role -ne 'anon') { throw 'Only a publishable or anon key is allowed.' }
}
$packageRoot = Join-Path $taskOut 'campus-quest-client'
New-Item -ItemType Directory -Path $packageRoot -Force | Out-Null
Copy-Item -LiteralPath $taskJar -Destination (Join-Path $packageRoot 'tetris.jar') -Force
$configLines = @(('server.url=' + $publicValues['server.url']),
    ('supabase.url=' + $publicValues['supabase.url']),
    ('supabase.publishableKey=' + $publicKey))
[IO.File]::WriteAllLines((Join-Path $packageRoot 'tetris-client.properties'), $configLines, [Text.UTF8Encoding]::new($false))
$launcher = @'
@echo off
cd /d "%~dp0"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" (
  start "" "%JAVA_HOME%\bin\javaw.exe" -jar "%~dp0tetris.jar"
  exit /b
)
where javaw.exe >nul 2>nul
if not errorlevel 1 (
  start "" javaw.exe -jar "%~dp0tetris.jar"
  exit /b
)
echo Java 8 or newer is required. Install a Java runtime, then run play.cmd again.
pause
'@
[IO.File]::WriteAllText((Join-Path $packageRoot 'play.cmd'), $launcher, [Text.Encoding]::ASCII)
$readme = @'
Tetris Monster / Campus Quest

Java 8 이상을 설치하고 play.cmd 또는 tetris.jar를 실행하세요.
대학교 → 졸업 → 취업의 3개 스테이지, 총 9전투입니다.
로컬 시작: 계정 없이 스토리·튜토리얼·연습·상점 이용.
온라인 로그인: 기존 계정 이메일/비밀번호 → Online Battle → 방 생성/입장 → 두 사람 READY.
PvP 랭킹: 서버가 확정한 공식 경기 전적만 표시. 로컬 PvE/LAN 기록은 제외.
회원가입·인증메일은 운영 서비스의 메일 설정에 따라 사용 가능 여부가 달라집니다.
Render Free 서버가 절전 후 재시작하면 운영자가 대전 접수를 다시 열어야 할 수 있습니다.

조작: ←→ 이동 / ↑↓ 회전 / D 한 칸 낙하 / SPACE 즉시 낙하 / C HOLD
P 로컬 일시정지 / 1~4 아이템 / ESC 돌아가기
로비의 설정에서 BGM/효과음 음량·음소거, LAN 접속, 5단계 조작 튜토리얼을 설정합니다.
첫 로컬 시작에서 튜토리얼을 직접 진행하며, 설정의 '다시 보지 않기'를 저장할 수 있습니다.
창은 크기를 조절할 수 있으며 플레이 영역 비율을 4:3으로 유지합니다.

진행·캐릭터·재화·소리·튜토리얼 선택은 사용자 홈 .tetris-monster에 저장됩니다.
기존 진행은 v2로 이관되며 첫 저장 전 .v1.bak 백업을 남깁니다.
자체 제작 몬스터 이미지와 University_Simulation 차용 이미지/효과음은 JAR에 포함됩니다.
공개 클라이언트 설정에는 서버 주소와 publishable key만 포함되며 계정 토큰은 저장하지 않습니다.
'@
[IO.File]::WriteAllText((Join-Path $packageRoot 'README.txt'), $readme, [Text.UTF8Encoding]::new($true))
$zip = Join-Path $taskOut 'campus-quest-client.zip'
# 폴더의 다른 파일이 섞이지 않도록 배포 파일을 명시한다.
$files = @('tetris.jar', 'tetris-client.properties', 'play.cmd', 'README.txt') | ForEach-Object { Join-Path $packageRoot $_ }
Compress-Archive -LiteralPath $files -DestinationPath $zip -Force
Write-Output "Client package: $zip"
Get-FileHash -LiteralPath $zip -Algorithm SHA256 | Select-Object Algorithm, Hash
