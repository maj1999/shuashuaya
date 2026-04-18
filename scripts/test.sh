#!/usr/bin/env bash
#
# 运行 shared + :update 模块的全平台测试
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_FILE="$PROJECT_ROOT/logs/test-common.log"

mkdir -p "$PROJECT_ROOT/logs"

echo "=== CleanPic: 运行 shared + :update 模块测试 ==="

cd "$PROJECT_ROOT"
./gradlew :shared:allTests :update:allTests "$@" 2>&1 | tee "$LOG_FILE"

echo "=== 测试完成（日志: $LOG_FILE）==="
