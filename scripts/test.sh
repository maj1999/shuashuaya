#!/usr/bin/env bash
#
# 运行 shared 模块的全平台测试
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== CleanPic: 运行 shared 模块测试 ==="

cd "$PROJECT_ROOT"
./gradlew :shared:allTests "$@"

echo "=== 测试完成 ==="
