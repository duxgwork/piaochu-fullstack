#!/usr/bin/env bash
set -euo pipefail
PIAOCHU_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$PIAOCHU_ROOT/scripts/java-env.sh"
cd "$PIAOCHU_ROOT/server"
# 默认独立使用8090端口和server/data/piaochu，不与原待办工程共用数据库。
exec ./mvnw spring-boot:run "$@"
