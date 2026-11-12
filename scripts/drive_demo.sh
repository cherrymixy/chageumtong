#!/usr/bin/env bash
# 실차 신호 경로(Pleos Vehicle SDK + CarPropertyManager) 검증용 주행 시나리오.
# 에뮬레이터가 user 빌드라 adb root / inject-vhal-event가 막혀 있어 Virtual Console HTTP API를 쓴다.
# 사용법: scripts/drive_demo.sh [속도km/h=60] [주행초=40]   (60km/h × 40초 ≈ 0.67km → 적립 가능)
set -euo pipefail
SPEED=${1:-60}
SECS=${2:-40}
V=http://127.0.0.1:8088

adb forward tcp:8088 tcp:8088 >/dev/null
# Virtual Console 세션은 약 3초 뒤 끊기므로 매 요청 전에 다시 연결한다
vc() { curl -sf -X POST "$V/browser/session?client=drive_demo" >/dev/null; curl -sf -X PUT "$V/vehicle/$1" >/dev/null; }
speed() { vc "controls?speed=$1&accelerator=0&brake=0"; }

echo "P → D"
speed 0; vc "gear?gear=Park"; sleep 2
vc "gear?gear=Drive"; sleep 1

echo "가속 → ${SPEED}km/h"
for s in 20 40 "$SPEED"; do speed "$s"; sleep 1; done

echo "${SECS}초 주행"
for ((i = 0; i < SECS; i++)); do speed "$SPEED"; sleep 1; done

echo "감속 → 정차 → P"
for s in 40 20 0; do speed "$s"; sleep 1; done
vc "gear?gear=Park"
echo "완료: 앱 상태가 ENDED인지 확인하세요"
