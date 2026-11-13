#!/usr/bin/env python3
"""
지도 화면 사용처 목록을 카카오에서 미리 받아 app/src/main/assets/places_kakao.json 으로 저장한다.
앱은 이 파일만 읽으므로 REST 키가 APK에 들어가지 않는다 (지도 중심이 고정 좌표라 결과가 항상 같음).

실행: python3 scripts/fetch_kakao_places.py   (local.properties 의 KAKAO_REST_KEY 사용)
- 장소: 카카오 로컬 API 키워드 검색, 분류별 가까운 순 최대 10곳
- 사진: Daum 이미지 검색 첫 결과의 주소만 저장 (사진 파일은 저장하지 않음, 앱이 실행 중에 불러옴)
"""
import json
import pathlib
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/assets/places_kakao.json"
CENTER = (37.5665, 126.9780)  # 서울시청 = AppViewModel.mapCenter 와 같아야 함
PER_CATEGORY = 10

# (분류, 검색어, 이름+카카오 분류명(공백 제거)에 들어 있어야 하는 글자, 카테고리 그룹 코드)
# 키워드 검색엔 다른 업종이 섞여 와서 거른다 (예: "현대자동차 지점" → 현대해상·미소금융)
SPECS = [
    ("CHARGER", "전기차충전소", "전기차충전소", None),
    ("DEALER", "현대자동차 지점", "자동차판매점>현대자동차", None),
    ("BLUEHANDS", "블루핸즈", "블루핸즈", None),
    ("GAS", "현대오일뱅크", "현대오일뱅크", "OL7"),
]


def rest_key():
    for line in (ROOT / "local.properties").read_text().splitlines():
        if line.startswith("KAKAO_REST_KEY="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("local.properties 에 KAKAO_REST_KEY 가 없어요")


def get(url, params, key):
    req = urllib.request.Request(f"{url}?{urllib.parse.urlencode(params)}", headers={"Authorization": f"KakaoAK {key}"})
    with urllib.request.urlopen(req, timeout=10) as r:
        return json.load(r)


def keep(doc, tag):
    return tag in (doc["place_name"] + doc.get("category_name", "")).replace(" ", "")


def to_place(doc, category):
    meters = int(doc.get("distance") or 0)
    return {
        "id": f"kakao-{doc['id']}",
        "category": category,
        "name": doc["place_name"],
        "subtitle": doc.get("road_address_name") or doc.get("address_name", ""),
        # 직선거리를 시속 30km(분당 500m)로 환산한 추정치
        "minutes": max(1, meters // 500),
        "meters": meters,
        "open": True,  # 로컬 API엔 영업시간이 없음
        "mapX": 0.0,
        "mapY": 0.0,
        "lat": float(doc["y"]),
        "lon": float(doc["x"]),
        "imageAsset": "",
        "imageUrl": None,
    }


def main():
    key = rest_key()
    places = []
    for category, query, tag, group in SPECS:
        params = {"query": query, "x": CENTER[1], "y": CENTER[0], "radius": 20000, "sort": "distance", "size": 15}
        if group:
            params["category_group_code"] = group
        docs = get("https://dapi.kakao.com/v2/local/search/keyword.json", params, key)["documents"]
        found = [to_place(d, category) for d in docs if keep(d, tag)][:PER_CATEGORY]
        print(f"{category}: {len(found)}곳")
        places += found
    for p in places:
        docs = get("https://dapi.kakao.com/v2/search/image", {"query": p["name"], "size": 1}, key)["documents"]
        p["imageUrl"] = docs[0]["image_url"] if docs else None
    print(f"사진 있음 {sum(p['imageUrl'] is not None for p in places)}/{len(places)}")
    OUT.write_text(json.dumps(places, ensure_ascii=False, indent=1) + "\n")
    print(f"저장: {OUT.relative_to(ROOT)}")


def _self_check():
    insurer = {"place_name": "현대해상 자동차보상기획파트", "category_name": "금융,보험 > 금융서비스 > 보험사"}
    dealer = {"place_name": "현대자동차 종로지점", "category_name": "교통,수송 > 자동차 > 자동차판매점 > 현대자동차"}
    assert not keep(insurer, "자동차판매점>현대자동차") and keep(dealer, "자동차판매점>현대자동차")
    gas = to_place({"id": "1", "place_name": "HD현대오일뱅크 시청주유소", "x": "126.978", "y": "37.566", "distance": "1250"}, "GAS")
    assert gas["minutes"] == 2 and gas["lat"] == 37.566 and gas["id"] == "kakao-1"


if __name__ == "__main__":
    _self_check()
    main()
