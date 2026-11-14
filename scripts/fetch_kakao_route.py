#!/usr/bin/env python3
"""
분석 화면 지도용 데모 경로를 카카오모빌리티 길찾기(카카오내비 경로 엔진)에서 미리 받아
app/src/main/assets/demo_route.json 의 "path" ([위도, 경도] 목록)로 저장한다.
앱은 이 파일만 읽으므로 REST 키가 APK에 들어가지 않는다.

실행: python3 scripts/fetch_kakao_route.py   (local.properties 의 KAKAO_REST_KEY 사용)
경로: 데모 라벨(종로구 → 중구 → 용산구)에 맞춘 출발·경유·도착. 데모 10km에 가깝게 경유지를 둠
"""
import json
import math
import pathlib
import re
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
ROUTE = ROOT / "app/src/main/assets/demo_route.json"
API = "https://apis-navi.kakaomobility.com/v1/directions"

# (경도, 위도) — 카카오모빌리티 API는 x=경도, y=위도 순서
ORIGIN = (126.9769, 37.5759)  # 종로구 광화문
WAYPOINTS = [
    (126.9918, 37.5704),  # 종로구 종로3가
    (126.9707, 37.5547),  # 중구 서울역
]
DESTINATION = (126.9716, 37.5414)  # 용산구 남영역
MIN_GAP_M = 15  # 이보다 가까운 점은 버려서 파일을 가볍게


def rest_key():
    for line in (ROOT / "local.properties").read_text().splitlines():
        if line.startswith("KAKAO_REST_KEY="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("local.properties 에 KAKAO_REST_KEY 가 없어요")


def xy(p):
    return f"{p[0]},{p[1]}"


def meters(a, b):
    """두 [위도, 경도] 사이 거리 (짧은 거리용 근사)"""
    dy = (a[0] - b[0]) * 111_320
    dx = (a[1] - b[1]) * 111_320 * math.cos(math.radians(a[0]))
    return math.hypot(dx, dy)


def main():
    params = {
        "origin": xy(ORIGIN),
        "destination": xy(DESTINATION),
        "waypoints": "|".join(xy(w) for w in WAYPOINTS),
        "priority": "RECOMMEND",
    }
    req = urllib.request.Request(f"{API}?{urllib.parse.urlencode(params)}", headers={"Authorization": f"KakaoAK {rest_key()}"})
    with urllib.request.urlopen(req, timeout=15) as r:
        data = json.load(r)

    route = data["routes"][0]
    if route.get("result_code", 0) != 0:
        raise SystemExit(f"길찾기 실패: {route.get('result_msg')}")

    path = []
    for section in route["sections"]:
        for road in section["roads"]:
            v = road["vertexes"]  # [x1, y1, x2, y2, ...]
            for i in range(0, len(v), 2):
                p = [round(v[i + 1], 6), round(v[i], 6)]
                if not path or meters(path[-1], p) >= MIN_GAP_M:
                    path.append(p)
    km = route["summary"]["distance"] / 1000

    text = ROUTE.read_text()
    line = f'  "path": {json.dumps(path, separators=(",", ":"))}'
    if '"path"' in text:
        text = re.sub(r'  "path": \[.*\]', line, text)
    else:
        text = re.sub(r"\]\s*\n\}\s*$", "],\n" + line + "\n}\n", text)
    json.loads(text)  # 깨진 JSON 저장 방지
    ROUTE.write_text(text)
    print(f"경로 {km:.1f}km, 점 {len(path)}개 → {ROUTE.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
