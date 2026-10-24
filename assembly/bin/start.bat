@echo off
chcp 65001 >nul 2>&1
title 宠物店管理系统
set SCRIPT_DIR=%~dp0
set "JAVA_HOME=%SCRIPT_DIR%jdk\windows"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%SCRIPT_DIR%logs" mkdir "%SCRIPT_DIR%logs"
if not exist "%SCRIPT_DIR%data\backup" mkdir "%SCRIPT_DIR%data\backup"

echo ============================================
echo   宠物店管理系统 v${project.version} 正在启动，请稍候...
echo   启动后访问 http://localhost:8080
echo   关闭此窗口即停止程序
echo ============================================
echo.

java -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai ^
  -jar "%SCRIPT_DIR%pet-shop.jar" ^
  --spring.config.additional-location=file:./config/

echo.
echo 程序已退出。若启动失败，请查看 logs\error.log
pause
