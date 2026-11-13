package com.pure.chageumtong.data.navi

import android.util.Log
import com.pure.chageumtong.BuildConfig
import com.pure.chageumtong.domain.Place
import com.pure.chageumtong.domain.PlaceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 카카오 로컬 API 키워드 검색으로 주변 포인트 사용처(충전소·대리점·블루핸즈·현대오일뱅크)를 찾는다.
 * GET https://dapi.kakao.com/v2/local/search/keyword.json, 헤더 `Authorization: KakaoAK {REST 키}`.
 * 카드 사진은 카카오 이미지 검색(GET /v2/search/image)의 첫 결과(없으면 사진 없이 표시). 블로그 사진이라 로고·지도 캡처가 섞일 수 있음.
 * REST 키가 없거나 요청이 실패하면 빈 목록 (지도 화면은 시드 장소를 보여줌).
 */
object PlaceSearch {
    private const val TAG = "PlaceSearch"
    private const val PER_CATEGORY = 10
    private val json = Json { ignoreUnknownKeys = true }

    /** tag: 장소 이름+카카오 분류명(공백 제거)에 들어 있어야 하는 글자. 키워드 검색엔 다른 업종이 섞여 와서 거른다 */
    internal enum class Spec(val category: PlaceCategory, val query: String, val tag: String, val groupCode: String?) {
        CHARGER(PlaceCategory.CHARGER, "전기차충전소", "전기차충전소", null),
        DEALER(PlaceCategory.DEALER, "현대자동차 지점", "자동차판매점>현대자동차", null),
        BLUEHANDS(PlaceCategory.BLUEHANDS, "블루핸즈", "블루핸즈", null),
        GAS(PlaceCategory.GAS, "현대오일뱅크", "현대오일뱅크", "OL7"),
    }

    @Serializable private data class Res(val documents: List<Doc> = emptyList())

    @Serializable private data class Doc(
        val id: String,
        @SerialName("place_name") val name: String,
        @SerialName("category_name") val categoryName: String = "",
        val x: String,
        val y: String,
        @SerialName("road_address_name") val road: String = "",
        @SerialName("address_name") val address: String = "",
        val distance: String = "",
    )

    @Serializable private data class ImageRes(val documents: List<ImageDoc> = emptyList())
    @Serializable private data class ImageDoc(@SerialName("image_url") val url: String)

    suspend fun nearby(lat: Double, lon: Double): List<Place> = withContext(Dispatchers.IO) {
        val key = BuildConfig.KAKAO_REST_KEY
        if (key.isBlank()) return@withContext emptyList()
        coroutineScope {
            val places = Spec.entries.map { s -> async { search(s, lat, lon, key) } }.awaitAll().flatten()
            // 사진은 장소마다 따로 검색 (실패한 곳은 사진 없음)
            places.map { p -> async { p.copy(imageUrl = imageFor(p.name, key)) } }.awaitAll()
        }
    }

    private fun search(s: Spec, lat: Double, lon: Double, key: String): List<Place> = runCatching {
        val q = URLEncoder.encode(s.query, "UTF-8")
        val group = s.groupCode?.let { "&category_group_code=$it" } ?: ""
        parse(get("https://dapi.kakao.com/v2/local/search/keyword.json?query=$q$group&x=$lon&y=$lat&radius=20000&sort=distance&size=15", key), s)
            .take(PER_CATEGORY)
    }.onFailure { Log.w(TAG, "search failed: ${s.query}", it) }.getOrDefault(emptyList())

    private fun imageFor(name: String, key: String): String? = runCatching {
        val q = URLEncoder.encode(name, "UTF-8")
        parseImage(get("https://dapi.kakao.com/v2/search/image?query=$q&size=1", key))
    }.onFailure { Log.w(TAG, "image search failed: $name", it) }.getOrNull()

    private fun get(url: String, key: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.setRequestProperty("Authorization", "KakaoAK $key")
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}: ${conn.errorStream?.bufferedReader()?.readText()}")
            return conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    internal fun parseImage(body: String): String? = json.decodeFromString<ImageRes>(body).documents.firstOrNull()?.url

    internal fun parse(body: String, s: Spec): List<Place> =
        json.decodeFromString<Res>(body).documents
            .filter { s.tag in (it.name + it.categoryName).replace(" ", "") }
            .map { d ->
                val meters = d.distance.toIntOrNull() ?: 0
                Place(
                    id = "kakao-${d.id}",
                    category = s.category,
                    name = d.name,
                    subtitle = d.road.ifBlank { d.address },
                    // shortcut: 직선거리를 시속 30km(분당 500m)로 환산한 추정치, 정확히 하려면 카카오모빌리티 길찾기 API로 교체
                    minutes = maxOf(1, meters / 500),
                    meters = meters,
                    // shortcut: 로컬 API엔 영업시간이 없어 영업중으로 표시, 영업 여부가 필요하면 별도 데이터 필요
                    open = true,
                    mapX = 0f,
                    mapY = 0f,
                    lat = d.y.toDouble(),
                    lon = d.x.toDouble(),
                    imageAsset = "", // 시드 그림 대신 검색 사진만 씀
                )
            }
}
