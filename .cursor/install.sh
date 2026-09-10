#!/usr/bin/env bash
#
# Cloud Agent install for Twigmark / Docear-Desktop.
#
# Idempotent, non-interactive. Safe to re-run. NEVER deploys, NEVER stops a
# running Twigmark/Docear instance (see .cursor/rules/no-deploy-build.mdc).
#
# Layers:
#   * Runtime  : JDK 8  -> the application's supported runtime (README "运行环境: Java 8").
#   * Compile  : JDK 21 -> modernization compile probe (source/target 1.8), matches
#                          .github/workflows/modernization-verify.yml.
#   * Build tool: Apache Ant is vendored in the repo (tools/apache-ant-1.10.14).
#   * GUI test : Xvfb + a lightweight WM + screenshot tools so the Swing desktop
#                app can be launched and exercised headlessly.
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SUDO=""
if [ "$(id -u)" -ne 0 ]; then
  SUDO="sudo"
fi

echo "==== Twigmark Cloud Agent install ===="

# --- System packages (idempotent) ---------------------------------------
export DEBIAN_FRONTEND=noninteractive
PKGS=(
  openjdk-8-jdk   # application runtime (Java 8)
  xvfb            # virtual framebuffer for headless GUI runs / tests
  fluxbox         # minimal window manager
  imagemagick     # screenshots (import)
  xdotool         # scripted GUI interaction for manual tests
  x11-apps        # xwd / misc X utilities
  ffmpeg          # screen recording for demos
  fonts-dejavu    # baseline fonts for legible rendering
)
$SUDO apt-get update -qq
$SUDO apt-get install -y -qq "${PKGS[@]}"

# --- Executable bits on repo scripts / vendored Ant ---------------------
chmod +x \
  scripts/verify-modernization.sh \
  scripts/verify-mm-fixtures.py \
  scripts/backup-mindmaps.sh \
  scripts/run-twigmark-linux.sh \
  tools/apache-ant-1.10.14/bin/ant 2>/dev/null || true

# --- Report detected toolchains -----------------------------------------
echo "---- toolchains ----"
if [ -x /usr/lib/jvm/java-8-openjdk-amd64/bin/java ]; then
  echo "Java 8 (runtime): $(/usr/lib/jvm/java-8-openjdk-amd64/bin/java -version 2>&1 | head -1)"
fi
if command -v java >/dev/null 2>&1; then
  echo "Default java    : $(java -version 2>&1 | head -1)"
fi
echo "Ant             : $("$ROOT/tools/apache-ant-1.10.14/bin/ant" -version 2>/dev/null || echo 'vendored at tools/apache-ant-1.10.14')"

echo "==== install complete ===="
