#!/usr/bin/env bash
# 팝업 3종 + 주행 위젯 시연 (Figma 97:2250). Virtual Console로 기어·속도를 바꾸고, 단계마다 Enter로 넘어간다.
# 사용법: scripts/popup_demo.sh
set -euo pipefail
ADB=${ADB:-$(command -v adb || echo ~/Library/Android/sdk/platform-tools/adb)}
PKG=com.pure.chageumtong
V=http://127.0.0.1:8088

"$ADB" forward tcp:8088 tcp:8088 >/dev/null
# Virtual Console 세션은 약 3초 뒤 끊기므로 매 요청 전에 다시 연결한다
vc() { curl -sf -X POST "$V/browser/session?client=popup_demo" >/dev/null; curl -sf -X PUT "$V/vehicle/$1" >/dev/null; }
speed() { vc "controls?speed=$1&accelerator=0&brake=0"; }
gear() { vc "gear?gear=$1"; }
# Enter를 누르면 바로 다음 단계. 입력을 못 받는 곳(Claude Code의 ! 실행 등)에선 $2초 기다렸다 자동으로 넘어감
step() {
    echo; echo "▶ $1"
    if ! read -r -t "$2" -p "  (Enter, 또는 $2초 뒤 자동) " _; then sleep "$2"; fi
}

"$ADB" shell appops set $PKG SYSTEM_ALERT_WINDOW allow </dev/null # 위젯 권한 (에뮬레이터엔 설정 화면이 없음)
speed 0; gear Park
"$ADB" shell am start -n $PKG/.MainActivity >/dev/null </dev/null

step "1) 주행 + 위젯: Enter 후 60초 달려요. 그동안 하단 [내비] 아이콘을 눌러 보세요 → 아래에 '이번 주행 포인트' 위젯" 3
gear Drive; sleep 1
# 70km/h × 60초 ≈ 1.2km → 1P. 기본 제한 60 + 10 이하라 과속 아님
for ((i = 0; i < 60; i++)); do speed 70; sleep 1; done
for s in 30 0; do speed "$s"; sleep 1; done
gear Park
# 정차·P 뒤에 차금통으로 돌아오기 (팝업은 앱 안에서 뜸). 주행 중엔 앱을 억지로 열지 않는다
"$ADB" shell am start -n $PKG/.MainActivity >/dev/null </dev/null
echo "  정차·P 완료 → 2초 뒤 '🚗 NP가 쌓였어요!' 팝업. 확인했으면 [닫기]를 누르세요 (적립하면 2번이 안 떠요)"

step "2) 재탑승: Enter 누르면 기어 N(정차) → '잊어버린 포인트가 있어요..' 팝업. 이번엔 [포인트 적립하기]를 누르세요" 10
gear Neutral

step "3) N모드 광고: 적립 후 N으로 서 있으면 5초 뒤 '잠시 정차 중 5P 받기!' → [광고 보고 5P받기] → 5초 후 +5P. 다 봤으면 Enter" 25
gear Park
echo "  끝. 기어 P로 돌려놨어요"
