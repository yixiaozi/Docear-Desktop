#!/bin/bash
# Docear Mac 版本发布脚本
set -e

# 配置
PROJECT_DIR="/Users/wangyang/Develop/Docear-Desktop"
TEMP_DIR="/Users/wangyang/Temp"
ANT_CMD="${PROJECT_DIR}/tools/apache-ant-1.10.14/bin/ant"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 帮助信息
show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Docear Mac 版本构建与发布脚本"
    echo ""
    echo "Options:"
    echo "  -h, --help          Show this help message and exit"
    echo "  --skip-build        Skip building and use existing build artifacts"
    echo "  --check             Check system requirements and exit"
    echo ""
    echo "System Requirements:"
    echo "  - Java Development Kit (JDK) 8"
    echo "  - Ant 1.10.14 (included in project)"
    echo ""
    echo "How to install JDK 8 on macOS:"
    echo "  1. Download Eclipse Temurin 8 from: https://adoptium.net/temurin/releases/?version=8"
    echo "  2. Install the .pkg file"
    echo "  3. Verify installation: /usr/libexec/java_home -V"
    echo "  4. Set JAVA_HOME: export JAVA_HOME=\$(/usr/libexec/java_home -v 1.8)"
    echo ""
}

# 修复 Docear.app
fix_docear_app() {
    local app_path="$1"
    echo -e "${YELLOW}正在修复 $app_path ...${NC}"
    
    # 1. 创建新的启动脚本
    cat > "$app_path/Contents/MacOS/Docear" << 'EOF'
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
    
    # 2. 设置执行权限
    chmod +x "$app_path/Contents/MacOS/Docear"
    
    # 3. 修改 Info.plist
    local plist_path="$app_path/Contents/Info.plist"
    
    # 尝试使用 PlistBuddy，否则使用 sed
    /usr/libexec/PlistBuddy -c "Set :CFBundleExecutable Docear" "$plist_path" 2>/dev/null || {
        sed -i '' 's/FreeplaneJavaApplicationStub/Docear/g' "$plist_path"
    }
    
    # 4. 删除旧的启动器
    rm -f "$app_path/Contents/MacOS/FreeplaneJavaApplicationStub"
    
    echo -e "${GREEN}✓ $app_path 修复完成！${NC}"
}

# 检查系统要求
check_requirements() {
    echo -e "${YELLOW}Checking system requirements...${NC}"
    echo ""
    
    # 检查 Java
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
        echo -e "${GREEN}✓ JAVA_HOME is set to: $JAVA_HOME${NC}"
    else
        echo -e "${RED}✗ JAVA_HOME not properly set or javac not found!${NC}"
        echo ""
        echo -e "${YELLOW}Checking installed Java versions...${NC}"
        /usr/libexec/java_home -V || true
        echo ""
        echo -e "${YELLOW}Please install JDK 8 and set JAVA_HOME!${NC}"
        echo "See help (-h) for more information"
        return 1
    fi
    
    # 检查 Ant
    if [ -x "$ANT_CMD" ]; then
        echo -e "${GREEN}✓ Ant found at: $ANT_CMD${NC}"
    elif command -v ant &> /dev/null; then
        echo -e "${GREEN}✓ System Ant found${NC}"
        ANT_CMD="ant"
    else
        echo -e "${RED}✗ Ant not found!${NC}"
        return 1
    fi
    
    echo ""
    echo -e "${GREEN}All requirements met!${NC}"
    return 0
}

# 主程序
main() {
    local skip_build=0
    local check_only=0
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            --skip-build)
                skip_build=1
                shift
                ;;
            --check)
                check_only=1
                shift
                ;;
            *)
                echo -e "${RED}Unknown option: $1${NC}"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 打印头部信息
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Docear Mac 版本构建与发布${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    
    # 设置 JAVA_HOME 如果未设置
    if [ -z "$JAVA_HOME" ]; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 1.8 2>/dev/null || /usr/libexec/java_home 2>/dev/null || true)
    fi
    
    # 检查是否仅检查需求
    if [ $check_only -eq 1 ]; then
        check_requirements
        exit $?
    fi
    
    # 检查需求
    if ! check_requirements; then
        exit 1
    fi
    
    DOCEAR_APP="$PROJECT_DIR/docear_framework/build4mac/Docear.app"
    
    # 检查是否需要构建
    if [ $skip_build -eq 0 ]; then
        echo ""
        echo -e "${YELLOW}开始构建 Docear...${NC}"
        echo ""
        
        # 先构建 freeplane_ant
        echo -e "${YELLOW}步骤 1/3: 构建 freeplane_ant...${NC}"
        cd "$PROJECT_DIR/freeplane_ant"
        mkdir -p bin dist
        
        # 如果源代码存在，尝试编译
        if [ -d src ]; then
            echo -e "${YELLOW}Compiling freeplane_ant...${NC}"
            "$JAVA_HOME/bin/javac" -cp "$PROJECT_DIR/tools/apache-ant-1.10.14/lib/ant.jar" -d bin src/org/freeplane/ant/*.java 2>/dev/null || true
            cd bin
            jar cvf ../dist/freeplaneant.jar org/ 2>/dev/null || true
            cd ..
        fi
        
        # 现在构建完整的 Docear
        echo ""
        echo -e "${YELLOW}步骤 2/3: 构建 Docear 应用...${NC}"
        cd "$PROJECT_DIR/docear_framework"
        "$ANT_CMD" -f ant/build.xml clean macosxapp || {
            echo -e "${YELLOW}Trying just 'macosxapp' without clean...${NC}"
            "$ANT_CMD" -f ant/build.xml macosxapp
        }
    fi
    
    # 验证构建成功
    if [ ! -d "$DOCEAR_APP" ]; then
        echo -e "${RED}错误: 构建失败，未找到 Docear.app 在 $DOCEAR_APP${NC}"
        exit 1
    fi
    
    echo ""
    echo -e "${YELLOW}步骤 3/3: 修复 Docear.app 启动器...${NC}"
    fix_docear_app "$DOCEAR_APP"
    
    echo ""
    echo -e "${GREEN}构建成功！${NC}"
    echo -e "${GREEN}应用位于: $DOCEAR_APP${NC}"
    
    # 复制到 Temp 目录
    echo ""
    echo -e "${YELLOW}正在复制到 $TEMP_DIR ...${NC}"
    mkdir -p "$TEMP_DIR"
    rm -rf "$TEMP_DIR/Docear.app"
    cp -R "$DOCEAR_APP" "$TEMP_DIR/"
    
    # 验证复制
    if [ -d "$TEMP_DIR/Docear.app" ]; then
        echo -e "${GREEN}========================================${NC}"
        echo -e "${GREEN}完成！${NC}"
        echo -e "${GREEN}Docear.app 已成功复制到: $TEMP_DIR/Docear.app${NC}"
        echo -e "${GREEN}========================================${NC}"
        
        # 提示如何运行
        echo ""
        echo -e "${YELLOW}您可以通过以下方式运行 Docear:${NC}"
        echo "  1. 在 Finder 中打开 $TEMP_DIR，双击 Docear.app"
        echo "  2. 或者在终端中运行: open $TEMP_DIR/Docear.app"
        echo "  3. 或者直接运行源应用: open $DOCEAR_APP"
    else
        echo -e "${RED}复制失败！${NC}"
        exit 1
    fi
}

# 运行主程序
main "$@"
