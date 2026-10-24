#!/bin/bash
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

ARCH=$(uname -m)
if [ "$ARCH" = "x86_64" ] || [ "$ARCH" = "amd64" ]; then
    JAVA_HOME=$SCRIPT_DIR/jdk/linux-x64
elif [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "arm64" ]; then
    JAVA_HOME=$SCRIPT_DIR/jdk/linux-arm64
else
    echo "Unsupported architecture: $ARCH"
    exit 1
fi

export PATH=$JAVA_HOME/bin:$PATH

mkdir -p $SCRIPT_DIR/logs
mkdir -p $SCRIPT_DIR/data/backup

echo "========================================"
echo "  宠物店管理系统 v${project.version} 启动中..."
echo "========================================"

nohup java -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai \
  -jar $SCRIPT_DIR/pet-shop.jar \
  --spring.config.additional-location=file:./config/ \
  > /dev/null 2>&1 &

echo $! > $SCRIPT_DIR/app.pid
echo "已启动 PID=$(cat $SCRIPT_DIR/app.pid)"
echo "访问 http://localhost:8080   日志 logs/console.log"
echo "使用 stop.sh 停止应用"

exit 0
