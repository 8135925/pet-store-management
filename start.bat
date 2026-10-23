@echo off
setlocal
chcp 65001 >nul
title 宠物店管理系统
cd /d "%~dp0"

set "APP_HOME=%~dp0"
set "JAR=%APP_HOME%pet-shop.jar"

if not exist "%JAR%" (
  echo [错误] 未找到 pet-shop.jar，请确认它与 start.bat 在同一目录。
  pause
  exit /b 1
)

set "JAVA_CMD=java"
if exist "%APP_HOME%jre\bin\java.exe" set "JAVA_CMD=%APP_HOME%jre\bin\java.exe"

"%JAVA_CMD%" -version >nul 2>&1
if errorlevel 1 (
  echo [错误] 未检测到 Java 运行环境。
  echo 请安装 JDK/JRE 1.8，或把 jre 文件夹放到本目录下。
  pause
  exit /b 1
)

if not exist "%APP_HOME%logs" mkdir "%APP_HOME%logs"
if not exist "%APP_HOME%data" mkdir "%APP_HOME%data"
if not exist "%APP_HOME%data\backup" mkdir "%APP_HOME%data\backup"

echo ============================================
echo   宠物店管理系统 正在启动，请稍候...
echo   启动后会自动打开浏览器
echo   关闭此窗口即停止程序
echo ============================================
echo.

rem 8 秒后自动打开浏览器（与前台进程并行）
start "" cmd /c "timeout /t 8 >nul & start http://localhost:8080"

"%JAVA_CMD%" -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai ^
  -jar "%JAR%" ^
  --spring.config.additional-location=file:./config/

echo.
echo 程序已退出。若启动失败，请查看 logs\error.log
pause
