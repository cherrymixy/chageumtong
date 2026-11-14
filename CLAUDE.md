# CLAUDE.md — 차금통 (Pleos Connect 차량용 앱)

이 파일은 Claude Code가 매 세션 시작 시 읽는 규칙서다. `docs/PRD.md`가 기획의 SSOT, 이 파일이 작업 방식의 SSOT.

## 1. 프로젝트
- 앱: 차금통 / 팀: 삼남매 / 패키지: `com.pure.chageumtong`
- 대상: Pleos Connect Emulator v2.1.0 (Automotive, API 34, 2560×1440, 160dpi, 가로 고정)
- 스택: Kotlin · Jetpack Compose(Material3) · Navigation Compose · ViewModel+StateFlow · DataStore+kotlinx.serialization · 수동 DI(AppContainer). **Hilt 쓰지 마.**
- minSdk 28 / compileSdk·targetSdk 37 (현재 AGP 9.5 템플릿 유지) / Gradle 8+

## 2. 폴더 구조 (지켜라)
```
app/src/main/java/com/pure/chageumtong/
├─ core/        # AppContainer, 공용 유틸
├─ domain/      # 순수 Kotlin: 모델, DriveSessionEngine, 점수 계산 (Android import 금지)
├─ data/
│  ├─ vehicle/  # VehicleDataSource 인터페이스, PleosVehicleDataSource, DemoVehicleDataSource
│  ├─ navi/     # NaviHelper 래퍼
│  ├─ repo/     # PointRepository, SessionRepository (DataStore)
│  └─ seed/     # 시드 데이터, products.json / places.json 로더
├─ ui/
│  ├─ theme/    # Color.kt, Type.kt, Dimens.kt(fig() 스케일 헬퍼) — 모든 수치는 여기서
│  ├─ components/
│  └─ screens/  # home/ analysis/ map/ shop/ history/
└─ MainActivity.kt
```

## 3. 절대 규칙
1. **요청한 범위만.** 다른 화면·파일 건드리지 마. 범위 밖 수정이 필요하면 먼저 말하고 허락받아.
2. **무거운 작업은 계획 먼저.** 스캐폴드, SDK 연동, 엔진, Figma 디자인 패스는 "계획 → 사용자 승인 → 코딩".
3. **로직 동결 후 디자인.** 디자인 패스(ui/) 중에는 `domain/`, `data/` 절대 수정 금지. 필요하면 멈추고 보고.
4. **Pleos API는 추측 금지.** 의존성 버전·패키지명·권한·메서드 시그니처는 반드시 공식 문서를 먼저 읽고 써라:
   - SDK 개요: https://document.pleos.ai/en/api-reference/connect-sdk-pleos/
   - Vehicle 설정: https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/project-setup
   - 튜토리얼: https://document.pleos.ai/en/docs/pleos-only/tutorial/vehicle-sdk
   - 호환성: https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability
   문서와 실제 빌드가 다르면 빌드 에러 메시지를 근거로 고치고, 무엇을 바꿨는지 보고해라.
5. **입력원 분리.** 화면과 엔진은 데이터가 실차에서 왔는지 데모에서 왔는지 몰라야 한다. 분기는 `VehicleDataSource` 구현체에서만.
6. **매 STEP 끝 = 빌드 성공 + 에뮬레이터 확인 + git commit.** 커밋 메시지는 `step N: 한 줄 요약`.
7. **주행 중 제한**(PRD §7)을 깨는 UI를 만들지 마라.
8. **Figma는 Dev Mode MCP로 수치를 직접 읽어라.** 스크린샷 눈대중 금지. `get_design_context` / `get_variable_defs` / `get_screenshot` 사용.
   - fileKey `N0XWXc1XGmwJ2IKMKcsQZZ`
   - 홈 `95:2142` · 분석 `95:2638` · 분석(칩 선택) `95:2689` · 지도 `95:2751` · 상점 `95:2870`
   - Figma px → dp 변환은 반드시 `Dimens.fig()` 사용 (세로 기준 s = 1210/1184, 앱 영역 1682×1210dp 실측). 하드코딩 금지.
   - 일러스트(유니콘·도시·당근)는 팀이 제공한 PNG/SVG(`app/src/main/res/drawable*`)만 쓴다. 벡터를 코드로 다시 그리지 마.
9. **서드파티 IP 금지.** 포켓몬·피너츠·잔망루피 등 캐릭터 이미지를 레포에 넣지 마 (PRD §9).
10. 에러가 나면: 원인 1줄 → 고친 내용 1줄 → 다시 빌드. 같은 에러 3번 반복되면 멈추고 사용자에게 선택지를 줘.

## 4. 테스트 도구
- 단위 테스트: `domain/` 점수·구간 계산은 반드시 JUnit 테스트 포함
- 차량 신호 주입: 에뮬레이터가 user 빌드라 `adb root`·`inject-vhal-event` 불가 (확인함). **Virtual Console HTTP API** 사용:
  ```
  adb forward tcp:8088 tcp:8088
  curl -X POST "http://127.0.0.1:8088/browser/session?client=me"   # 세션 유지(약 3초 만료 → 매 요청 전 호출)
  curl -X PUT  "http://127.0.0.1:8088/vehicle/gear?gear=Drive"        # Park|Reverse|Neutral|Drive
  curl -X PUT  "http://127.0.0.1:8088/vehicle/controls?speed=60&accelerator=0&brake=0"  # km/h
  ```
  앱이 받는 값: 기어 VENDOR_GEAR_SELECTION (N=1, R=2, P=4, D=8), 속도 PERF_VEHICLE_SPEED(0x11600207, m/s). 시나리오는 `scripts/`에 쉘 스크립트로.
- 새 패키지를 처음 설치한 직후엔 D에서 "주행 중에는 사용할 수 없습니다"가 뜰 수 있음 → `adb install -r`로 한 번 더 설치.

## 5. 빌드 순서
STEP 0 계획 → 1 스캐폴드+SDK → 2 도메인 엔진 → 3 입력원(Real/Demo)+세션 → 4 저장·시드 → 5 회색박스 전체 화면 → 6 디자인 패스(홈·분석) → 7 릴리즈·README → (Phase 2) 8 디자인 패스(지도·상점·내역) → 9 NaviHelper·TTS → 10 주행 제한 QA → 11 PT 준비
