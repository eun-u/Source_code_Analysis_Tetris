param(
    [ValidateSet('Build', 'Test', 'Run', 'Server', 'GuiTest', 'Preview', 'NetworkFixture')]
    [string]$Task = 'Build',
    [string]$JdkHome = '',
    [switch]$AllowVisibleDesktop,
    [ValidateRange(1, 65535)]
    [int]$Port = 28080
)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot

# 개발자 검증의 창·포커스·OS 입력 사용 제외 및 명시적 옵션을 통한 수동 GUI 검증 허용
if (($Task -eq 'GuiTest' -or $Task -eq 'Preview') -and -not $AllowVisibleDesktop) {
    throw 'This task opens visible windows. Use -Task Test for headless verification; manual desktop testing requires -AllowVisibleDesktop.'
}

function Find-Jdk {
    if ($JdkHome) {
        if (-not (Test-Path -LiteralPath (Join-Path $JdkHome 'bin\javac.exe'))) {
            throw "No javac.exe in requested JDK: $JdkHome"
        }
        return (Resolve-Path -LiteralPath $JdkHome).Path
    }
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) { $candidates += Split-Path (Split-Path $compiler.Source) }
    foreach ($base in @("$env:USERPROFILE\.jdks", 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Java', 'C:\Program Files\Microsoft')) {
        if (Test-Path -LiteralPath $base) {
            $candidates += @(Get-ChildItem -LiteralPath $base -Directory | ForEach-Object FullName)
        }
    }
    # 기존 검증용 로컬 JDK 탐색 및 다운로드·설치 제외
    $candidates += Join-Path $env:TEMP 'codex-tetris-baseline-jdk8\jdk8u504-b01'
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath (Join-Path $candidate 'bin\javac.exe')) { return $candidate }
    }
    throw 'JDK 8+ required. Set JAVA_HOME or pass -JdkHome <path>. A JRE alone can run the JAR but cannot build it.'
}

function Reset-OutputDirectory([string]$path) {
    $resolvedTarget = [IO.Path]::GetFullPath($path)
    $allowedRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot 'out\g0'))
    if (-not $resolvedTarget.StartsWith($allowedRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to clean output outside g0: $resolvedTarget"
    }
    foreach ($ancestor in @((Join-Path $projectRoot 'out'), $allowedRoot, $resolvedTarget)) {
        if ((Test-Path -LiteralPath $ancestor) -and ((Get-Item -LiteralPath $ancestor).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw "Refusing linked output directory: $ancestor"
        }
    }
    if (Test-Path -LiteralPath $resolvedTarget) { Remove-Item -LiteralPath $resolvedTarget -Recurse -Force }
    New-Item -ItemType Directory -Path $resolvedTarget -Force | Out-Null
}

function Compile-Sources([string]$sourceRoot, [string]$destination, [string]$classpath, [string]$listName) {
    $files = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter '*.java' | Sort-Object FullName)
    if ($files.Count -eq 0) { throw "No Java sources: $sourceRoot" }
    $argumentFile = Join-Path $projectRoot "out\g0\$listName"
    $lines = @($files | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    [IO.File]::WriteAllLines($argumentFile, $lines, (New-Object Text.UTF8Encoding($false)))
    $compilerArguments = @('-encoding', 'UTF-8', '-source', '8', '-target', '8', '-d', $destination)
    if ($classpath) { $compilerArguments += @('-cp', $classpath) }
    $compilerArguments += "@$argumentFile"
    & $javac @compilerArguments
    if ($LASTEXITCODE -ne 0) { throw "Compilation failed: $sourceRoot (exit $LASTEXITCODE)" }
    Write-Output "Compiled $($files.Count) sources: $sourceRoot"
}

$selectedJdk = Find-Jdk
$javac = Join-Path $selectedJdk 'bin\javac.exe'
$java = Join-Path $selectedJdk 'bin\java.exe'
$jar = Join-Path $selectedJdk 'bin\jar.exe'
Write-Output "JDK: $selectedJdk"
$classes = Join-Path $projectRoot 'out\g0\classes'
$testClasses = Join-Path $projectRoot 'out\g0\test-classes'
Reset-OutputDirectory $classes
Compile-Sources (Join-Path $projectRoot 'src\main\java') $classes '' 'main-sources.txt'
Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src\main\resources') | Copy-Item -Destination $classes -Recurse -Force
$jarPath = Join-Path $projectRoot 'out\tetris.jar'
& $jar cfe $jarPath kr.ac.jbnu.se.tetris.Tetris -C $classes .
if ($LASTEXITCODE -ne 0) { throw "JAR creation failed (exit $LASTEXITCODE)" }
Write-Output "JAR: $jarPath"

if ($Task -in @('Test', 'GuiTest', 'Preview', 'NetworkFixture')) {
    Reset-OutputDirectory $testClasses
    Compile-Sources (Join-Path $projectRoot 'src\test\java') $testClasses $classes 'test-sources.txt'
    $testResources = Join-Path $projectRoot 'src\test\resources'
    if (Test-Path -LiteralPath $testResources) {
        Get-ChildItem -LiteralPath $testResources | Copy-Item -Destination $testClasses -Recurse -Force
    }
    $testClasspath = "$classes;$testClasses"
    if ($Task -eq 'Preview') {
        & $java '-Djava.awt.headless=false' -cp $testClasspath kr.ac.jbnu.se.tetris.app.SeongeunApplication
        if ($LASTEXITCODE -ne 0) { throw 'UI preview failed' }
        exit 0
    }
    if ($Task -eq 'NetworkFixture') {
        & $java '-Djava.awt.headless=true' -ea -cp $testClasspath kr.ac.jbnu.se.tetris.support.OnlinePreviewScenario
        if ($LASTEXITCODE -ne 0) { throw 'Network fixture failed' }
        exit 0
    }
    $testFiles = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src\test\java') -Recurse -Filter '*Test.java' | Sort-Object FullName)
    if ($testFiles.Count -eq 0) { throw 'No test entrypoints were found' }
    foreach ($test in $testFiles) {
        $content = Get-Content -LiteralPath $test.FullName -Raw -Encoding UTF8
        if ($content -notmatch '(?m)^package\s+([\w.]+)\s*;') { throw "Missing test package: $($test.FullName)" }
        $className = $Matches[1] + '.' + $test.BaseName
        & $java '-Djava.awt.headless=true' -ea -cp $testClasspath $className
        if ($LASTEXITCODE -ne 0) { throw "Test failed: $className (exit $LASTEXITCODE)" }
        Write-Output "PASS suite: $className"
    }
    Write-Output "PASS: $($testFiles.Count) headless test suites"
    if ($Task -eq 'GuiTest') {
        & $java '-Djava.awt.headless=false' -ea -cp $testClasspath kr.ac.jbnu.se.tetris.ui.DesktopSmoke (Join-Path $projectRoot 'out\g0')
        if ($LASTEXITCODE -ne 0) { throw "Desktop smoke failed (exit $LASTEXITCODE)" }
    }
}
if ($Task -eq 'Run') {
    & $java -jar $jarPath
    if ($LASTEXITCODE -ne 0) { throw "Game failed (exit $LASTEXITCODE)" }
}
if ($Task -eq 'Server') {
    & $java '-Djava.awt.headless=true' -cp $jarPath kr.ac.jbnu.se.tetris.network.server.LocalGameServer $Port
    if ($LASTEXITCODE -ne 0) { throw "Local server failed (exit $LASTEXITCODE)" }
}
