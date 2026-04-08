#!/usr/bin/env bash
# =============================================================================
# EduVerse Academy - Local Tomcat Deployment Script (Linux / macOS)
#
# This script:
#   1. Checks prerequisites (Java 11+, Maven, CATALINA_HOME)
#   2. Builds the WAR artifact with Maven
#   3. Copies the WAR into the Tomcat webapps directory
#   4. Prints instructions to start Tomcat
#
# Usage:
#   chmod +x deployment/tomcat-setup.sh
#   ./deployment/tomcat-setup.sh
# =============================================================================

set -euo pipefail

# ---- Colours for output ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Colour

info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ---- Navigate to project root (parent of deployment/) ----
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"
info "Project directory: $PROJECT_DIR"

# ============================================================
# 1. Check prerequisites
# ============================================================

# -- Java --
if ! command -v java &>/dev/null; then
    error "Java is not installed or not on PATH."
    error "Install JDK 11+ and try again."
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}' | cut -d. -f1)
if [[ "$JAVA_VER" -lt 11 ]]; then
    error "Java 11 or higher is required (found version $JAVA_VER)."
    exit 1
fi
info "Java version OK (major version: $JAVA_VER)"

# -- Maven --
if ! command -v mvn &>/dev/null; then
    error "Maven is not installed or not on PATH."
    error "Install Maven 3.6+ and try again."
    exit 1
fi
info "Maven found: $(mvn --version | head -1)"

# -- Tomcat (CATALINA_HOME) --
if [[ -z "${CATALINA_HOME:-}" ]]; then
    error "CATALINA_HOME is not set."
    error "Set it to your Tomcat installation directory, e.g.:"
    error "  export CATALINA_HOME=/opt/tomcat9"
    exit 1
fi

if [[ ! -d "$CATALINA_HOME/webapps" ]]; then
    error "CATALINA_HOME ($CATALINA_HOME) does not contain a webapps directory."
    exit 1
fi
info "Tomcat found at: $CATALINA_HOME"

# ============================================================
# 2. Build the WAR
# ============================================================

info "Building EduVerse Academy WAR with Maven..."
mvn clean package -DskipTests -B

WAR_FILE="$PROJECT_DIR/target/eduverse-academy.war"
if [[ ! -f "$WAR_FILE" ]]; then
    error "Build succeeded but WAR file not found at $WAR_FILE"
    exit 1
fi
info "WAR built successfully: $WAR_FILE"

# ============================================================
# 3. Deploy to Tomcat
# ============================================================

info "Copying WAR to $CATALINA_HOME/webapps/ ..."
cp "$WAR_FILE" "$CATALINA_HOME/webapps/eduverse-academy.war"
info "WAR deployed to Tomcat webapps directory."

# ============================================================
# 4. Instructions
# ============================================================

echo ""
echo "============================================================"
echo "  EduVerse Academy - Deployment Complete"
echo "============================================================"
echo ""
echo "  WAR location : $CATALINA_HOME/webapps/eduverse-academy.war"
echo ""
echo "  To start Tomcat:"
echo "    $CATALINA_HOME/bin/startup.sh"
echo ""
echo "  To stop Tomcat:"
echo "    $CATALINA_HOME/bin/shutdown.sh"
echo ""
echo "  Application URL:"
echo "    http://localhost:8080/eduverse-academy"
echo ""
echo "  Make sure PostgreSQL is running with:"
echo "    Database : eduverse"
echo "    User     : postgres"
echo "    Password : postgres"
echo "    Port     : 5432"
echo ""
echo "============================================================"
