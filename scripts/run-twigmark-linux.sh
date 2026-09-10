#!/usr/bin/env bash
#
# Run Twigmark / Docear-Desktop headlessly on Linux (e.g. a Cloud Agent VM).
#
# This is a DEVELOPMENT / TESTING helper only:
#   * It compiles the Freeplane core assembly into the repo's build/ folder and
#     launches it from there under an Xvfb virtual display.
#   * It NEVER deploys to an install directory and NEVER stops a running
#     Twigmark/Docear instance (see .cursor/rules/no-deploy-build.mdc).
#
# The application's supported runtime is Java 8 (README "运行环境: Java 8"). The
# non-modernized plugins (script/latex/svg/formula/...) are skipped for this
# minimal, runnable core; the modernized subset compiles under any JDK >= 8
# targeting 1.8 bytecode.
#
# Usage:
#   scripts/run-twigmark-linux.sh              # build if needed, then launch
#   scripts/run-twigmark-linux.sh --rebuild    # force a clean core rebuild
#   DISPLAY=:0 scripts/run-twigmark-linux.sh   # use an existing X display
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

ANT="$ROOT/tools/apache-ant-1.10.14/bin/ant"
BUILD_DIR="$ROOT/freeplane_framework/build"
FRAMEWORK_JAR="$BUILD_DIR/framework.jar"

# --- Pick a runtime JVM: Java 8 preferred, otherwise fall back -----------
pick_runtime() {
  for h in /usr/lib/jvm/java-8-openjdk-amd64 /usr/lib/jvm/java-1.8.0-openjdk-amd64; do
    [ -x "$h/bin/java" ] && { echo "$h"; return 0; }
  done
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then echo "$JAVA_HOME"; return 0; fi
  command -v java >/dev/null 2>&1 && { echo "$(dirname "$(dirname "$(command -v java)")")"; return 0; }
  return 1
}

# --- Pick a JDK for compiling (needs javac) ------------------------------
pick_jdk() {
  for h in /usr/lib/jvm/java-21-openjdk-amd64 /usr/lib/jvm/java-8-openjdk-amd64 /usr/lib/jvm/java-1.8.0-openjdk-amd64; do
    [ -x "$h/bin/javac" ] && { echo "$h"; return 0; }
  done
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then echo "$JAVA_HOME"; return 0; fi
  return 1
}

REBUILD=0
[ "${1:-}" = "--rebuild" ] && REBUILD=1

if [ "$REBUILD" = "1" ] || [ ! -f "$FRAMEWORK_JAR" ]; then
  JDK_HOME="$(pick_jdk)" || { echo "ERROR: no JDK with javac found" >&2; exit 1; }
  echo "Building Freeplane core with JDK: $JDK_HOME"
  JAVA_HOME="$JDK_HOME" PATH="$JDK_HOME/bin:$PATH" \
    "$ANT" -f freeplane_framework/ant/build.xml build \
      -Dskip_latex=true -Dskip_svg=true -Dskip_script=true \
      -Dskip_formula=true -Dskip_bugreport=true \
      -Dskip_remote_client=true -Dskip_remote_server=true
fi

RT_HOME="$(pick_runtime)" || { echo "ERROR: no Java runtime found (install openjdk-8-jdk)" >&2; exit 1; }
echo "Runtime JVM: $RT_HOME ($("$RT_HOME/bin/java" -version 2>&1 | head -1))"

# --- Ensure an X display -------------------------------------------------
STARTED_XVFB=0
if [ -z "${DISPLAY:-}" ]; then
  export DISPLAY=:99
  if ! xdpyinfo -display "$DISPLAY" >/dev/null 2>&1; then
    echo "Starting Xvfb on $DISPLAY"
    Xvfb "$DISPLAY" -screen 0 1360x900x24 -nolisten tcp >/tmp/xvfb-twigmark.log 2>&1 &
    STARTED_XVFB=$!
    sleep 2
    command -v fluxbox >/dev/null 2>&1 && { fluxbox >/tmp/fluxbox-twigmark.log 2>&1 & }
    sleep 1
  fi
fi

echo "Launching Twigmark on DISPLAY=$DISPLAY ..."
cd "$BUILD_DIR"
exec "$RT_HOME/bin/java" -Xmx768m \
  -Dorg.knopflerfish.framework.bundlestorage=memory \
  -Dorg.freeplane.globalresourcedir="$BUILD_DIR/resources" \
  -Dorg.knopflerfish.gosg.jars=reference:file:"$BUILD_DIR/core/" \
  -Dorg.freeplane.main.application.FreeplaneStarter.headless=false \
  -Djava.awt.headless=false \
  -jar "$BUILD_DIR/framework.jar" \
  -xargs "$BUILD_DIR/props.xargs" \
  -xargs "$BUILD_DIR/init.xargs"
