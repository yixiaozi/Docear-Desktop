#!/bin/bash

# 修复 Docear Mac 应用的启动问题

set -e

if [ $# -eq 0 ]; then
    echo "Usage: $0 /path/to/Docear.app"
    exit 1
fi

APP="$1"

if [ ! -d "$APP" ]; then
    echo "Error: $APP not found"
    exit 1
fi

echo "Fixing Docear app at $APP"

# 1. 创建新的启动脚本
cat > "$APP/Contents/MacOS/Docear" << 'EOF'
#!/bin/bash

# 找到应用包的 Contents 目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONTENTS_DIR="$(dirname "$SCRIPT_DIR")"
JAVA_DIR="$CONTENTS_DIR/Resources/Java"

# 查找合适的 Java
if [ -x "/usr/libexec/java_home" ]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 1.8+ 2>/dev/null || /usr/libexec/java_home 2>/dev/null)"
    if [ -n "$JAVA_HOME" ]; then
        export JAVA_HOME
        PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

# 进入 Java 目录
cd "$JAVA_DIR" || exit 1

# 启动 Docear
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
EOF

chmod +x "$APP/Contents/MacOS/Docear"

# 2. 修改 Info.plist
PLIST="$APP/Contents/Info.plist"

# 使用 PlistBuddy 来修改
/usr/libexec/PlistBuddy -c "Set :CFBundleExecutable Docear" "$PLIST" 2>/dev/null || {
    # 如果 PlistBuddy 失败，使用简单的文本替换
    sed -i '' 's/FreeplaneJavaApplicationStub/Docear/' "$PLIST"
}

echo "Fix completed successfully!"
echo "You can now launch $APP"
