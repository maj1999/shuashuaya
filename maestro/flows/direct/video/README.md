# Video E2E Tests

These tests require video files on the emulator/device to run.
They are separated from the main `flows/` directory so that
`maestro test maestro/flows/` only runs photo-based tests by default.

## Prerequisites

Push at least one MP4 video to the emulator before running:

```bash
adb push test-video.mp4 /sdcard/DCIM/Camera/
```

## Running

```bash
maestro test maestro/flows/video/
```
