#!/usr/bin/env bash
#
# 生成测试用媒体资源（需要 ffmpeg）
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ASSETS_DIR="$PROJECT_ROOT/test-assets"

if ! command -v ffmpeg &>/dev/null; then
    echo "错误: 需要 ffmpeg。请运行: brew install ffmpeg"
    exit 1
fi

mkdir -p "$ASSETS_DIR/photos" "$ASSETS_DIR/videos"

echo "=== 生成测试照片 ==="

COLORS=("#4A90D9" "#D94A4A" "#4AD97A" "#D9A84A" "#8A4AD9"
        "#D94A8A" "#4AD9D9" "#90D94A" "#D9D94A" "#4A4AD9"
        "#D96A3A" "#3AD97A")

for i in $(seq -w 1 12); do
    color="${COLORS[$((10#$i - 1))]}"
    ffmpeg -y -f lavfi -i "color=c=${color}:s=1080x1920:d=1" \
        -update 1 -frames:v 1 "$ASSETS_DIR/photos/test_${i}.jpg" 2>/dev/null
    echo "  test_${i}.jpg (1080x1920, ${color})"
done

echo "=== 生成测试视频 ==="

ffmpeg -y -f lavfi -i "color=c=#D9A84A:s=1920x1080:d=10:r=30" \
    -c:v libx264 -preset fast -crf 23 \
    "$ASSETS_DIR/videos/test_01.mp4" 2>/dev/null
echo "  test_01.mp4 (1080p, 10s)"

ffmpeg -y -f lavfi -i "color=c=#8A4AD9:s=1280x720:d=5:r=30" \
    -c:v libx264 -preset fast -crf 23 \
    "$ASSETS_DIR/videos/test_02.mp4" 2>/dev/null
echo "  test_02.mp4 (720p, 5s)"

echo "=== 测试媒体生成完成 ==="
ls -lh "$ASSETS_DIR/photos/" "$ASSETS_DIR/videos/"
