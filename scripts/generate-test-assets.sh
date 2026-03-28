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

ffmpeg -y -f lavfi -i "color=c=#4A90D9:s=1080x1920:d=1" \
    -vf "drawtext=text='test_01 1080x1920':fontsize=60:fontcolor=white:x=(w-tw)/2:y=(h-th)/2" \
    -frames:v 1 "$ASSETS_DIR/photos/test_01.jpg" 2>/dev/null
echo "  test_01.jpg (1080x1920)"

ffmpeg -y -f lavfi -i "color=c=#D94A4A:s=2160x3840:d=1" \
    -vf "drawtext=text='test_02 2160x3840':fontsize=120:fontcolor=white:x=(w-tw)/2:y=(h-th)/2" \
    -frames:v 1 -q:v 2 "$ASSETS_DIR/photos/test_02.jpg" 2>/dev/null
echo "  test_02.jpg (2160x3840)"

ffmpeg -y -f lavfi -i "color=c=#4AD97A:s=720x1280:d=1" \
    -vf "drawtext=text='test_03 720x1280':fontsize=40:fontcolor=white:x=(w-tw)/2:y=(h-th)/2" \
    -frames:v 1 "$ASSETS_DIR/photos/test_03.png" 2>/dev/null
echo "  test_03.png (720x1280)"

echo "=== 生成测试视频 ==="

ffmpeg -y -f lavfi -i "color=c=#D9A84A:s=1920x1080:d=10:r=30" \
    -vf "drawtext=text='test_01 1080p %{pts\:hms}':fontsize=60:fontcolor=white:x=(w-tw)/2:y=(h-th)/2" \
    -c:v libx264 -preset fast -crf 23 \
    "$ASSETS_DIR/videos/test_01.mp4" 2>/dev/null
echo "  test_01.mp4 (1080p, 10s)"

ffmpeg -y -f lavfi -i "color=c=#8A4AD9:s=3840x2160:d=30:r=30" \
    -vf "drawtext=text='test_02 4K %{pts\:hms}':fontsize=120:fontcolor=white:x=(w-tw)/2:y=(h-th)/2" \
    -c:v libx264 -preset fast -crf 28 \
    "$ASSETS_DIR/videos/test_02.mp4" 2>/dev/null
echo "  test_02.mp4 (4K, 30s)"

echo "=== 测试媒体生成完成 ==="
ls -lh "$ASSETS_DIR/photos/" "$ASSETS_DIR/videos/"
