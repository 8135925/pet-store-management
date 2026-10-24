#!/bin/bash
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

if [ -f $SCRIPT_DIR/app.pid ]; then
  PID=$(cat $SCRIPT_DIR/app.pid)
  kill $PID && echo "已停止 PID=$PID"
  # 10 秒未退出则强制终止
  for i in $(seq 1 10); do
    kill -0 $PID 2>/dev/null || exit 0
    sleep 1
  done
  kill -9 $PID 2>/dev/null
  echo "已强制停止"
  rm -f $SCRIPT_DIR/app.pid
else
  echo "未找到 app.pid"
  ps -ef | grep pet-shop.jar | grep -v grep | awk '{print $2}' | xargs -r kill
  echo "已按进程名停止 pet-shop.jar"
fi
