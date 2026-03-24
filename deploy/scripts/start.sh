#!/usr/bin/env sh

set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
APP_HOME="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
PID_FILE="$APP_HOME/run/eam.pid"
LOG_OUT="$APP_HOME/logs/app.out.log"
LOG_ERR="$APP_HOME/logs/app.err.log"
JAR_FILE="$APP_HOME/eam-app.jar"
CONFIG_DIR="$APP_HOME/config/"

mkdir -p "$APP_HOME/logs" "$APP_HOME/run"

if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "EAM 后端已在运行，PID=$(cat "$PID_FILE")"
  exit 0
fi

nohup java -jar "$JAR_FILE" \
  --spring.profiles.active=prod \
  --spring.config.additional-location="file:$CONFIG_DIR" \
  >"$LOG_OUT" 2>"$LOG_ERR" &

echo $! >"$PID_FILE"
echo "EAM 后端已启动，PID=$(cat "$PID_FILE")"
