#!/usr/bin/env sh

set -eu

APP_HOME="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
FRONTEND_HOME="$(CDPATH= cd -- "$APP_HOME/../dg_admin_web_next" && pwd)"
RELEASE_DIR="$APP_HOME/release/eam-backend"
MAVEN_REPO_LOCAL="${MAVEN_REPO_LOCAL:-}"
MAVEN_SETTINGS_FILE="${MAVEN_SETTINGS_FILE:-}"

cd "$APP_HOME"

if [ -z "$MAVEN_REPO_LOCAL" ]; then
  MAVEN_REPO_LOCAL="$HOME/.m2/repository"
fi

mkdir -p "$MAVEN_REPO_LOCAL"

if [ "${SKIP_BUILD:-false}" != "true" ]; then
  (
    cd "$FRONTEND_HOME"
    npm run build
  )

  if [ -n "$MAVEN_SETTINGS_FILE" ]; then
    mvn -gs "$MAVEN_SETTINGS_FILE" clean package -DskipTests "-Dmaven.repo.local=$MAVEN_REPO_LOCAL"
  else
    mvn clean package -DskipTests "-Dmaven.repo.local=$MAVEN_REPO_LOCAL"
  fi
fi

rm -rf "$RELEASE_DIR"
mkdir -p "$RELEASE_DIR/config" "$RELEASE_DIR/scripts" "$RELEASE_DIR/sql/migration" "$RELEASE_DIR/systemd" "$RELEASE_DIR/windows"

JAR_FILE="$(find "$APP_HOME/target" -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)"

if [ -z "$JAR_FILE" ]; then
  echo "No publishable JAR file was found."
  exit 1
fi

cp "$JAR_FILE" "$RELEASE_DIR/eam-app.jar"
cp "$APP_HOME/deploy/config/application-prod.yml" "$RELEASE_DIR/config/application-prod.yml"
cp "$APP_HOME/deploy/sql/README.md" "$RELEASE_DIR/sql/README.md"
cp "$APP_HOME/src/main/resources/db/migration/"*.sql "$RELEASE_DIR/sql/migration/"
cp "$APP_HOME/deploy/scripts/start.sh" "$RELEASE_DIR/scripts/start.sh"
cp "$APP_HOME/deploy/scripts/stop.sh" "$RELEASE_DIR/scripts/stop.sh"
cp "$APP_HOME/deploy/scripts/start.ps1" "$RELEASE_DIR/scripts/start.ps1"
cp "$APP_HOME/deploy/scripts/stop.ps1" "$RELEASE_DIR/scripts/stop.ps1"
cp "$APP_HOME/deploy/systemd/eam.service" "$RELEASE_DIR/systemd/eam.service"
cp "$APP_HOME/deploy/windows/install-service.ps1" "$RELEASE_DIR/windows/install-service.ps1"

chmod +x "$RELEASE_DIR/scripts/start.sh" "$RELEASE_DIR/scripts/stop.sh"

echo "Backend release package created: $RELEASE_DIR"
echo "Maven local repository: $MAVEN_REPO_LOCAL"
if [ -n "$MAVEN_SETTINGS_FILE" ]; then
  echo "Maven settings file: $MAVEN_SETTINGS_FILE"
else
  echo "Maven settings file: using default system settings.xml"
fi
