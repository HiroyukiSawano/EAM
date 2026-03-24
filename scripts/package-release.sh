#!/usr/bin/env sh

set -eu

APP_HOME="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
RELEASE_DIR="$APP_HOME/release/eam-backend"

cd "$APP_HOME"

if [ "${SKIP_BUILD:-false}" != "true" ]; then
  mvn clean package -DskipTests
fi

rm -rf "$RELEASE_DIR"
mkdir -p "$RELEASE_DIR/config" "$RELEASE_DIR/scripts" "$RELEASE_DIR/sql/migration" "$RELEASE_DIR/systemd" "$RELEASE_DIR/windows"

JAR_FILE="$(find "$APP_HOME/target" -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)"

if [ -z "$JAR_FILE" ]; then
  echo "未找到可发布的 JAR 文件"
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

echo "后端发布包已生成：$RELEASE_DIR"
