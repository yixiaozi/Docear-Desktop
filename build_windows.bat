@echo off
chcp 65001 >nul
echo ========================================
echo   Docear Windows 专用构建脚本
echo   (仅生成 Windows 版本，跳过 Mac/Linux)
echo ========================================
echo.

cd /d "%~dp0"

echo [1/3] 清理旧构建...
call ant -f freeplane_framework/ant/build.xml clean

echo.
echo [2/3] 构建核心 + Windows 分发包...
call ant -f freeplane_framework/ant/build.xml binzip installer portableinstaller -Dskip_mac=true -Dskip_dmg=true

echo.
echo [3/3] 构建完成！
echo.
echo 输出目录: %cd%\freeplane_framework\dist
echo.
pause
