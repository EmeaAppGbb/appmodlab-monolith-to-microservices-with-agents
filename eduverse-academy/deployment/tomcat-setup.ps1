# =============================================================================
# EduVerse Academy - Local Tomcat Deployment Script (Windows / PowerShell)
#
# This script:
#   1. Checks prerequisites (Java 11+, Maven, CATALINA_HOME)
#   2. Builds the WAR artifact with Maven
#   3. Copies the WAR into the Tomcat webapps directory
#   4. Prints instructions to start Tomcat
#
# Usage:
#   .\deployment\tomcat-setup.ps1
# =============================================================================

$ErrorActionPreference = "Stop"

function Write-Info  { param([string]$Msg) Write-Host "[INFO]  $Msg" -ForegroundColor Green }
function Write-Warn  { param([string]$Msg) Write-Host "[WARN]  $Msg" -ForegroundColor Yellow }
function Write-Err   { param([string]$Msg) Write-Host "[ERROR] $Msg" -ForegroundColor Red }

# ---- Navigate to project root (parent of deployment\) ----
$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectDir = Split-Path -Parent $ScriptDir
Set-Location $ProjectDir
Write-Info "Project directory: $ProjectDir"

# ============================================================
# 1. Check prerequisites
# ============================================================

# -- Java --
try {
    $javaOutput = & java -version 2>&1 | Select-Object -First 1
    if ($javaOutput -match '"(\d+)') {
        $javaMajor = [int]$Matches[1]
    } else {
        throw "Unable to parse Java version."
    }
} catch {
    Write-Err "Java is not installed or not on PATH."
    Write-Err "Install JDK 11+ and try again."
    exit 1
}

if ($javaMajor -lt 11) {
    Write-Err "Java 11 or higher is required (found version $javaMajor)."
    exit 1
}
Write-Info "Java version OK (major version: $javaMajor)"

# -- Maven --
try {
    $mvnVersion = & mvn --version 2>&1 | Select-Object -First 1
} catch {
    Write-Err "Maven is not installed or not on PATH."
    Write-Err "Install Maven 3.6+ and try again."
    exit 1
}
Write-Info "Maven found: $mvnVersion"

# -- Tomcat (CATALINA_HOME) --
$catalinaHome = $env:CATALINA_HOME
if (-not $catalinaHome) {
    Write-Err "CATALINA_HOME environment variable is not set."
    Write-Err "Set it to your Tomcat installation directory, e.g.:"
    Write-Err '  $env:CATALINA_HOME = "C:\apache-tomcat-9"'
    exit 1
}

if (-not (Test-Path "$catalinaHome\webapps")) {
    Write-Err "CATALINA_HOME ($catalinaHome) does not contain a webapps directory."
    exit 1
}
Write-Info "Tomcat found at: $catalinaHome"

# ============================================================
# 2. Build the WAR
# ============================================================

Write-Info "Building EduVerse Academy WAR with Maven..."
& mvn clean package -DskipTests -B
if ($LASTEXITCODE -ne 0) {
    Write-Err "Maven build failed with exit code $LASTEXITCODE."
    exit 1
}

$warFile = Join-Path $ProjectDir "target\eduverse-academy.war"
if (-not (Test-Path $warFile)) {
    Write-Err "Build succeeded but WAR file not found at $warFile"
    exit 1
}
Write-Info "WAR built successfully: $warFile"

# ============================================================
# 3. Deploy to Tomcat
# ============================================================

$destination = Join-Path $catalinaHome "webapps\eduverse-academy.war"
Write-Info "Copying WAR to $destination ..."
Copy-Item -Path $warFile -Destination $destination -Force
Write-Info "WAR deployed to Tomcat webapps directory."

# ============================================================
# 4. Instructions
# ============================================================

Write-Host ""
Write-Host "============================================================"
Write-Host "  EduVerse Academy - Deployment Complete"
Write-Host "============================================================"
Write-Host ""
Write-Host "  WAR location : $destination"
Write-Host ""
Write-Host "  To start Tomcat:"
Write-Host "    $catalinaHome\bin\startup.bat"
Write-Host ""
Write-Host "  To stop Tomcat:"
Write-Host "    $catalinaHome\bin\shutdown.bat"
Write-Host ""
Write-Host "  Application URL:"
Write-Host "    http://localhost:8080/eduverse-academy"
Write-Host ""
Write-Host "  Make sure PostgreSQL is running with:"
Write-Host "    Database : eduverse"
Write-Host "    User     : postgres"
Write-Host "    Password : postgres"
Write-Host "    Port     : 5432"
Write-Host ""
Write-Host "============================================================"
