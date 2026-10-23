#!/bin/bash
APP_HOME=$(cd "$(dirname "$0")" && pwd)
cd "$APP_HOME"
mkdir -p logs data/backup

JAVA_CMD=java
if [ -x "$APP_HOME/jre/bin/java" ]; then
  JAVA_CMD="$APP_HOME/jre/bin/java"
fi

if ! "$JAVA_CMD" -version >/dev/null 2>&1; then
  echo "[错误] 未检测到 Java，请安装 JDK/JRE 1.8"
  exit 1
fi

nohup "$JAVA_CMD" -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai \
  -jar pet-shop.jar \
  --spring.config.additional-location=file:./config/ \
  > logs/console.log 2>&1 &

echo $! > app.pid
echo "已启动 PID=$(cat app.pid)"
echo "访问 http://localhost:8080   日志 logs/console.log"
