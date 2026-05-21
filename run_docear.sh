#!/bin/bash

# Docear 直接运行脚本 - 不依赖 macOS 应用包装

# 设置项目目录
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
DOCEAR_DIR="$PROJECT_DIR/docear_framework/build4mac/Docear.app/Contents/Resources/Java"

# 查找合适的 Java
if [ -x "/usr/libexec/java_home" ]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 1.8+ 2>/dev/null || /usr/libexec/java_home 2>/dev/null)"
    if [ -n "$JAVA_HOME" ]; then
        export JAVA_HOME
        PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

echo "使用 JAVA_HOME: $JAVA_HOME"
echo "运行目录: $DOCEAR_DIR"

# 进入 Java 目录
cd "$DOCEAR_DIR" || exit 1

# 启动 Docear
echo "正在启动 Docear..."
exec java \
    -Xmx512m \
    -Dorg.knopflerfish.framework.bundlestorage=memory \
    -Dorg.freeplane.globalresourcedir=./resources \
    -Dorg.knopflerfish.gosg.jars=reference:file:./core/ \
    -cp "framework.jar" \
    org.knopflerfish.framework.Main \
    -xargs ./props.xargs \
    -xargs ./init.xargs \
    "$@"
