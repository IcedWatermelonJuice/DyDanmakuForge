[CmdletBinding()]
param(
    [switch]$Clean
)

$jdkHome = $env:JAVA_17_HOME
if ([string]::IsNullOrWhiteSpace($jdkHome)) {
    $jdkHome = [Environment]::GetEnvironmentVariable('JAVA_17_HOME', 'User')
}
if ([string]::IsNullOrWhiteSpace($jdkHome)) {
    $jdkHome = [Environment]::GetEnvironmentVariable('JAVA_17_HOME', 'Machine')
}
if ([string]::IsNullOrWhiteSpace($jdkHome)) {
    throw '未设置 JAVA_17_HOME，请将其指向 JDK 17 根目录。'
}
$jdkHome = [Environment]::ExpandEnvironmentVariables($jdkHome).TrimEnd('\', '/')
$javaExe = Join-Path $jdkHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
    throw "JAVA_17_HOME 无效，未找到：$javaExe"
}

$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
try {
    $env:JAVA_HOME = $jdkHome
    $env:Path = "$(Join-Path $jdkHome 'bin');$previousPath"
    Write-Host "[DyDanmaku] Forge 1.20.1 使用 JAVA_HOME=$env:JAVA_HOME"
    if ($Clean) {
        & "$PSScriptRoot\gradlew.bat" clean build --no-daemon --console=plain
    } else {
        & "$PSScriptRoot\gradlew.bat" build --no-daemon --console=plain
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Forge 1.20.1 构建失败，退出码：$LASTEXITCODE"
    }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
}
