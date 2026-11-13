package com.pure.chageumtong.data.seed

import android.content.Context
import com.pure.chageumtong.domain.Place
import com.pure.chageumtong.domain.Product
import kotlinx.serialization.json.Json

data class Catalog(val products: List<Product>, val places: List<Place>, val kakaoPlaces: List<Place>)

/**
 * assets/products.json, assets/places.json (PRD §9 이미지 정책: 서드파티 IP 없음)
 * assets/places_kakao.json: 카카오 검색으로 미리 받은 실제 사용처 (scripts/fetch_kakao_places.py)
 */
object CatalogLoader {
    fun load(context: Context): Catalog {
        fun read(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }
        return Catalog(
            products = Json.decodeFromString(read("products.json")),
            places = Json.decodeFromString(read("places.json")),
            kakaoPlaces = Json.decodeFromString(read("places_kakao.json")),
        )
    }
}
