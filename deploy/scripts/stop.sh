#!/usr/bin/env sh

set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
APP_HOME="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
PID_FILE="$APP_HOME/run/eam.pid"

if [ ! -f "$PID_FILE" ]; then
  echo "未找到 PID 文件，无需停止"
  exit 0
fi

PID="$(cat "$PID_FILE")"

if kill -0 "$PID" 2>/dev/null; then
  kill "$PID"
  echo "已发送停止信号，PID=$PID"
else
  echo "PID=$PID 对应进程不存在"
fi

rm -f "$PID_FILE"
