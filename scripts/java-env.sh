#!/usr/bin/env bash
# 可由启动脚本 source；仅设置当前进程环境，不修改用户全局 Java 配置。
if [[ -z "${JAVA_HOME:-}" && -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" ]]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
if ! java -version >/dev/null 2>&1; then
  echo "未找到 JDK。请安装 JDK 17+，或设置 JAVA_HOME 指向 Android Studio 的 jbr。" >&2
  return 1 2>/dev/null || exit 1
fi
