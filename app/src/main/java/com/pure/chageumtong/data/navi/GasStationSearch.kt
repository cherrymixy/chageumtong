package com.pure.chageumtong.data.navi

import android.util.Log
import com.pure.chageumtong.BuildConfig
import com.pure.chageumtong.domain.Place
import com.pure.chageumtong.domain.PlaceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 카카오 로컬 API 키워드 검색으로 주변 현대오일뱅크 주유소를 찾는다.
 * GET https://dapi.kakao.com/v2/local/search/keyword.json, 헤더 `Authorization: KakaoAK {REST 키}`, 주유소 분류 OL7.
 * REST 키가 없거나 요청이 실패하면 빈 목록 (지도 화면은 시드 장소만 보여줌).
 */
object GasStationSearch {
    private const val TAG = "GasStationSearch"
    private const val BRAND = "현대오일뱅크"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable private data class Res(val documents: List<Doc> = emptyList())

    @Serializable private data class Doc(
        val id: String,
        @SerialName("place_name") val name: String,
        val x: String,
        val y: String,
        @SerialName("road_address_name") val road: String = "",
        @SerialName("address_name") val address: String = "",
        val distance: String = "",
    )

    suspend fun nearby(lat: Double, lon: Double): List<Place> = withContext(Dispatchers.IO) {
        val key = BuildConfig.KAKAO_REST_KEY
        if (key.isBlank()) return@withContext emptyList()
        runCatching {
            val q = URLEncoder.encode(BRAND, "UTF-8")
            val url = URL(
                "https://dapi.kakao.com/v2/local/search/keyword.json" +
                    "?query=$q&category_group_code=OL7&x=$lon&y=$lat&radius=20000&sort=distance&size=15",
            )
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 5_000
                conn.readTimeout = 5_000
                conn.setRequestProperty("Authorization", "KakaoAK $key")
                if (conn.responseCode != 200) error("HTTP ${conn.responseCode}: ${conn.errorStream?.bufferedReader()?.readText()}")
                parse(conn.inputStream.bufferedReader().readText())
            } finally {
                conn.disconnect()
            }
        }.onFailure { Log.w(TAG, "search failed", it) }.getOrDefault(emptyList())
    }

    /** 키워드 검색은 비슷한 이름(다른 브랜드)도 섞여 오므로 이름에 브랜드가 있는 곳만 남긴다 */
    internal fun parse(body: String): List<Place> =
        json.decodeFromString<Res>(body).documents
            .filter { BRAND in it.name.replace(" ", "") }
            .map { d ->
                val meters = d.distance.toIntOrNull() ?: 0
                Place(
                    id = "kakao-${d.id}",
                    category = PlaceCategory.GAS,
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
                    imageAsset = "place_gas",
                )
            }
}
