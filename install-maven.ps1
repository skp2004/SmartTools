[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ProgressPreference = 'SilentlyContinue'

$mavenVersion = "3.9.9"
$mvnDir = Join-Path $PSScriptRoot ".mvn"
$mavenHome = Join-Path $mvnDir "apache-maven-$mavenVersion"
$mvnCmd = Join-Path $mavenHome "bin\mvn.cmd"

if (Test-Path $mvnCmd) {
    Write-Host "Maven $mavenVersion already installed."
    exit 0
}

Write-Host "Downloading Maven $mavenVersion..."
$zipFile = Join-Path $mvnDir "maven.zip"
if (-not (Test-Path $mvnDir)) { New-Item -ItemType Directory -Path $mvnDir -Force | Out-Null }

$url = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
try {
    Invoke-WebRequest -Uri $url -OutFile $zipFile
    Write-Host "Extracting..."
    Expand-Archive -Path $zipFile -DestinationPath $mvnDir -Force
    Remove-Item $zipFile -Force -ErrorAction SilentlyContinue
    Write-Host "Maven $mavenVersion installed to $mavenHome"
} catch {
    Write-Error "Failed to download Maven: $_"
    exit 1
}
