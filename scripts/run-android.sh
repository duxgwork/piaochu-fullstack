#!/usr/bin/env bash
set -euo pipefail
PIAOCHU_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$PIAOCHU_ROOT/scripts/java-env.sh"
PIAOCHU_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$PIAOCHU_SDK" && -d "$HOME/Library/Android/sdk" ]]; then PIAOCHU_SDK="$HOME/Library/Android/sdk"; fi
if [[ -z "$PIAOCHU_SDK" && -d "$HOME/Android/Sdk" ]]; then PIAOCHU_SDK="$HOME/Android/Sdk"; fi
if [[ ! -x "$PIAOCHU_SDK/platform-tools/adb" ]]; then echo '请设置ANDROID_HOME或从Android Studio运行工程。' >&2;exit 1;fi
export ANDROID_HOME="$PIAOCHU_SDK"
PIAOCHU_ADB="$PIAOCHU_SDK/platform-tools/adb"
"$PIAOCHU_ADB" get-state >/dev/null
cd "$PIAOCHU_ROOT/android"
./gradlew :app:assembleDebug
"$PIAOCHU_ADB" install -r app/build/outputs/apk/debug/app-debug.apk
"$PIAOCHU_ADB" reverse tcp:8090 tcp:8090
"$PIAOCHU_ADB" shell am start -n com.piaochu.app.debug/com.piaochu.app.presentation.MainActivity
