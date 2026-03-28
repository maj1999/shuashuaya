#!/usr/bin/env bash
#
# HarmonyOS 平台测试：构建 HAP → 部署到模拟器
#
# [待启用] 需要 ohosArm64 Gradle target 解除注释后才可运行。
# 当前状态：脚本框架已就绪，构建命令待确认。
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_FILE="$PROJECT_ROOT/logs/test-harmony.log"

mkdir -p "$PROJECT_ROOT/logs"

echo "=== CleanPic: HarmonyOS 平台测试 ===" | tee "$LOG_FILE"
echo "时间: $(date)" | tee -a "$LOG_FILE"

# 检查 ohosArm64 target 是否启用
if ! grep -q "^[[:space:]]*ohosArm64" "$PROJECT_ROOT/shared/build.gradle.kts"; then
    echo "警告: ohosArm64 Gradle target 尚未启用。" | tee -a "$LOG_FILE"
    echo "请在 shared/build.gradle.kts 中取消 ohosArm64 块的注释。" | tee -a "$LOG_FILE"
    echo "此脚本将在 target 启用后可用。" | tee -a "$LOG_FILE"
    echo "" | tee -a "$LOG_FILE"
    echo "如需 AGC 云测试，请运行: scripts/emulators/harmony-cloud.sh" | tee -a "$LOG_FILE"
    exit 0
fi

# 检查 hdc
if ! command -v hdc &>/dev/null; then
    echo "错误: hdc 未找到。请安装 DevEco Studio 并配置 PATH。" | tee -a "$LOG_FILE"
    exit 1
fi

# 构建（具体 task 名称待 target 启用后确认）
echo "--- 构建 HarmonyOS HAP ---"
cd "$PROJECT_ROOT"
# TODO: 确认构建任务名称，可能是 :ohosApp:assembleDebug 或类似
echo "TODO: HarmonyOS 构建任务待确认" | tee -a "$LOG_FILE"

echo "=== HarmonyOS 测试完成（日志: $LOG_FILE）==="
