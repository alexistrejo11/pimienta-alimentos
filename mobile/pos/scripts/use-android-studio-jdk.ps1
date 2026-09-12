# Configure the current PowerShell session to use Android Studio's embedded JDK.
$candidates = @(
    "$env:USERPROFILE\.jdks\openjdk-26.0.2.1",
    "$env:ProgramFiles\Android\Android Studio\jbr",
    "$env:ProgramFiles\Android\Android Studio\jre",
    "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
    "$env:LOCALAPPDATA\Programs\Android Studio\jre"
)

$jdk = $candidates | Where-Object { Test-Path (Join-Path $_ "bin\java.exe") } | Select-Object -First 1
if (-not $jdk) {
    throw "No encontré el JDK embebido de Android Studio. Configura Gradle JDK desde Android Studio o ajusta la ruta en este script."
}

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
# Keep Gradle caches in the current user's profile.
if (-not $env:GRADLE_USER_HOME) {
    $env:GRADLE_USER_HOME = "$env:USERPROFILE\.gradle"
}
Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "GRADLE_USER_HOME=$env:GRADLE_USER_HOME"
& "$jdk\bin\java.exe" -version
