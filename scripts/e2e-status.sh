#!/usr/bin/env bash
# 定时汇报 E2E / 发布状态：读取最新的 logs/e2e-*.log，汇总通过/失败 + 进程存活 + 是否已发布。
# 供 cc-connect cron --exec 每 5 分钟调用。
set -uo pipefail
cd "$(dirname "$0")/.." || exit 0

ts="$(date '+%H:%M:%S')"

# 找最新的 e2e 日志
log="$(ls -t logs/e2e-*.log 2>/dev/null | head -1)"
if [ -z "${log:-}" ]; then
  echo "[$ts] E2E 汇报：暂无 E2E 日志。"
  exit 0
fi

# maestro 进程是否在跑
if ps aux | grep -iE '/\.maestro/lib' | grep -v grep >/dev/null 2>&1; then
  running="▶️ 运行中"
else
  running="⏹ 已结束"
fi

passed="$(grep -cE '\[Passed\]' "$log" 2>/dev/null)"; passed="${passed:-0}"
failed="$(grep -cE '\[Failed\]' "$log" 2>/dev/null)"; failed="${failed:-0}"
fails="$(grep -E '\[Failed\]' "$log" 2>/dev/null | sed 's/.*\[Failed\] //' | head -10)"
done_marker="$(grep -c 'ALL DONE' "$log" 2>/dev/null)"; done_marker="${done_marker:-0}"

# 是否已发布（GitHub release 最新 tag）
rel="$(gh release view --json tagName -q .tagName 2>/dev/null || echo '')"

echo "[$ts] E2E 汇报（cron）"
echo "日志: $(basename "$log")  $running"
echo "✅ 通过 $passed  ❌ 失败 $failed"
if [ -n "$fails" ]; then
  echo "失败明细:"
  echo "$fails" | sed 's/^/  - /'
fi
[ "$done_marker" != "0" ] && echo "本轮已跑完 (ALL DONE)"
[ -n "$rel" ] && echo "当前线上 Release: $rel"
