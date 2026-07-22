[CmdletBinding()]
param(
    [switch]$Clean
)

$jdkHome = $env:JAVA_21_HOME
if ([string]::IsNullOrWhiteSpace($jdkHome)) {
    $jdkHome = [Environment]::GetEnvironmentVariable('JAVA_21_HOME', 'User')
}
if ([string]::IsNullOrWhiteSpace($jdkHome)) {
    $jdkHome = [Environment]::GetEnvironmentVariable('JAVA_21_HOME', 'Machine')
}
if ([string]::IsNullOrWhiteSpace($jdkHome)) {
    throw 'JAVA_21_HOME is not set. Point it to the JDK 21 directory.'
}
$jdkHome = [Environment]::ExpandEnvironmentVariables($jdkHome).TrimEnd([char[]]@('\', '/'))
$javaExe = Join-Path $jdkHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
    throw "Invalid JAVA_21_HOME; java.exe was not found: $javaExe"
}

$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
try {
    $env:JAVA_HOME = $jdkHome
    $env:Path = "$(Join-Path $jdkHome 'bin');$previousPath"
    Write-Host "[DyDanmaku] Forge 1.21.1 JAVA_HOME=$env:JAVA_HOME"
    if ($Clean) {
        & "$PSScriptRoot\gradlew.bat" clean build --no-daemon --console=plain
    } else {
        & "$PSScriptRoot\gradlew.bat" build --no-daemon --console=plain
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Forge 1.21.1 build failed with exit code $LASTEXITCODE."
    }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
}
