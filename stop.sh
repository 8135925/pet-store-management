#!/bin/bash
cd "$(dirname "$0")"
if [ -f app.pid ]; then
  kill "$(cat app.pid)" && echo "已停止"
  rm -f app.pid
else
  echo "未找到 app.pid"
fi
