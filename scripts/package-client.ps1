param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
if (-not $SkipBuild) { & (Join-Path $root 'build.ps1') -Task Test }
$jar = Join-Path $root 'out\tetris.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Build the verified client JAR first.' }
$staging = Join-Path $root 'out\client-package'
New-Item -ItemType Directory -Path $staging -Force | Out-Null
Copy-Item -LiteralPath $jar -Destination (Join-Path $staging 'tetris.jar') -Force
Copy-Item -LiteralPath (Join-Path $root 'tetris-client.properties.example') -Destination $staging -Force
$launch = @'
@echo off
cd /d "%~dp0"
start "Tetris" javaw -jar "%~dp0tetris.jar"
'@
[IO.File]::WriteAllText((Join-Path $staging 'play.cmd'), $launch, [Text.Encoding]::ASCII)
$readme = @'
Tetris (Java 8 이상 필요)

play.cmd 또는 java -jar tetris.jar로 실행합니다.
온라인 서비스 공개 주소가 정해지면 tetris-client.properties.example을
tetris-client.properties로 복사하고 서버 URL, Supabase URL, publishable key를 채웁니다.
운영자 secret key와 admin token은 이 폴더에 넣지 않습니다.

온라인: 계정 · 랭킹에서 가입/메일 인증/로그인 후 온라인 랭킹 접속.
첫 참가자가 방을 만들고 방 번호를 상대에게 전달하면 두 명이 준비 후 대전합니다.
저장이 완료된 경기만 랭킹에 반영됩니다. 재실행 시 다시 로그인합니다.
튜토리얼·스토리·로컬 연습 기록은 공식 랭킹에 반영되지 않습니다.
'@
[IO.File]::WriteAllText((Join-Path $staging 'README.txt'), $readme, (New-Object Text.UTF8Encoding($true)))
# 명시한 공개 파일만 압축. 이전 실행의 로컬 설정이나 임의 파일은 포함하지 않음.
$files = @('tetris.jar','tetris-client.properties.example','play.cmd','README.txt') | ForEach-Object { Join-Path $staging $_ }
$archive = Join-Path $root 'out\tetris-client.zip'
Compress-Archive -LiteralPath $files -DestinationPath $archive -Force
Get-FileHash -LiteralPath $archive -Algorithm SHA256 | Select-Object Path, Hash
