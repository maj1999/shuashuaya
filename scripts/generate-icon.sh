#!/usr/bin/env bash
#
# 生成 Android App 图标（各 mipmap 尺寸）
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RES_DIR="$PROJECT_ROOT/androidApp/src/main/res"

if ! command -v ffmpeg &>/dev/null; then
    echo "错误: 需要 ffmpeg"
    exit 1
fi

# Android mipmap 尺寸（标准图标 + 自适应前景）
DIRS=("mipmap-mdpi" "mipmap-hdpi" "mipmap-xhdpi" "mipmap-xxhdpi" "mipmap-xxxhdpi")
ICON_SIZES=(48 72 96 144 192)
FG_SIZES=(108 162 216 324 432)

echo "=== 生成 CleanPic App 图标 ==="

for i in "${!DIRS[@]}"; do
    dir="${DIRS[$i]}"
    size="${ICON_SIZES[$i]}"
    fg="${FG_SIZES[$i]}"
    mkdir -p "$RES_DIR/$dir"

    # 标准图标：紫色渐变
    ffmpeg -y -f lavfi \
        -i "gradients=s=${size}x${size}:c0=#FFF8E1:c1=#FFECB3:duration=1:speed=0.01" \
        -update 1 -frames:v 1 \
        "$RES_DIR/$dir/ic_launcher.png" 2>/dev/null

    # 圆形图标
    cp "$RES_DIR/$dir/ic_launcher.png" "$RES_DIR/$dir/ic_launcher_round.png"

    # 自适应图标前景层（渐变 + 留白安全区）
    ffmpeg -y -f lavfi \
        -i "gradients=s=${fg}x${fg}:c0=#FFF8E1:c1=#FFECB3:duration=1:speed=0.01" \
        -update 1 -frames:v 1 \
        "$RES_DIR/$dir/ic_launcher_foreground.png" 2>/dev/null

    echo "  $dir: icon=${size} foreground=${fg}"
done

# 生成 Play Store 高分辨率图标（512x512）
mkdir -p "$RES_DIR/../ic_launcher-playstore"
ffmpeg -y -f lavfi \
    -i "gradients=s=512x512:c0=#FFF8E1:c1=#FFECB3:duration=1:speed=0.01" \
    -update 1 -frames:v 1 \
    "$RES_DIR/../ic_launcher-playstore.png" 2>/dev/null
echo "  Play Store: 512x512"

echo "=== 图标生成完成 ==="
