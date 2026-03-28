#!/usr/bin/env bash
#
# 华为 AGC 云测试/云调试引导
#
# 用法:
#   scripts/emulators/harmony-cloud.sh             — 显示操作指引
#   scripts/emulators/harmony-cloud.sh --open      — 直接打开 AGC 控制台
#
set -euo pipefail

echo "=== CleanPic: AGC 云测试 ==="
echo ""
echo "AGC 云测试操作指引："
echo "  1. 打开浏览器访问: https://developer.huawei.com/consumer/cn/agconnect/cloud-test/"
echo "  2. 登录华为开发者账号"
echo "  3. 上传 HAP 包进行测试"
echo ""
echo "AGC 云调试（远程真机）："
echo "  1. 打开浏览器访问: https://developer.huawei.com/consumer/cn/agconnect/cloud-adjust/"
echo "  2. 选择 HarmonyOS 设备"
echo "  3. 每日免费 300 分钟"
echo ""

if [ "${1:-}" = "--open" ] && command -v open &>/dev/null; then
    open "https://developer.huawei.com/consumer/cn/agconnect/cloud-test/"
fi
