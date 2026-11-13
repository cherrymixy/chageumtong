package com.pure.chageumtong.data

import com.pure.chageumtong.data.navi.PlaceSearch
import com.pure.chageumtong.domain.PlaceCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceSearchTest {
    @Test
    fun parse_gas_keepsOnlyBrandAndMapsFields() {
        val body = """
            {"meta":{"total_count":2},"documents":[
              {"id":"1","place_name":"HD현대오일뱅크 시청주유소","category_name":"교통,수송 > 자동차 > 주유소","x":"126.978","y":"37.566","road_address_name":"서울 중구 세종대로 1","address_name":"서울 중구 태평로1가","distance":"1250","phone":""},
              {"id":"2","place_name":"GS칼텍스 종로주유소","x":"126.99","y":"37.57","road_address_name":"","address_name":"서울 종로구","distance":"300"}
            ]}
        """.trimIndent()
        val places = PlaceSearch.parse(body, PlaceSearch.Spec.GAS)
        assertEquals(1, places.size)
        val p = places[0]
        assertEquals("kakao-1", p.id)
        assertEquals(PlaceCategory.GAS, p.category)
        assertEquals("서울 중구 세종대로 1", p.subtitle)
        assertEquals(1250, p.meters)
        assertEquals(2, p.minutes)
        assertEquals(37.566, p.lat, 1e-9)
        assertEquals(126.978, p.lon, 1e-9)
    }

    @Test
    fun parse_dealer_filtersByKakaoCategory() {
        // 실제 "현대자동차 지점" 검색 결과처럼 보험사·금융재단이 섞여 옴
        val body = """
            {"documents":[
              {"id":"1","place_name":"현대해상 자동차보상기획파트","category_name":"금융,보험 > 금융서비스 > 보험사 > 손해보험 > 현대해상화재보험","x":"1","y":"2","distance":"521"},
              {"id":"2","place_name":"현대자동차 종로지점","category_name":"교통,수송 > 자동차 > 자동차판매점 > 현대자동차","x":"1","y":"2","distance":"891"}
            ]}
        """.trimIndent()
        val places = PlaceSearch.parse(body, PlaceSearch.Spec.DEALER)
        assertEquals(listOf("현대자동차 종로지점"), places.map { it.name })
        assertEquals("place_dealer", places[0].imageAsset)
    }

    @Test
    fun parseImage_firstResultOrNull() {
        assertEquals("https://a/1.jpg", PlaceSearch.parseImage("""{"documents":[{"image_url":"https://a/1.jpg","width":1},{"image_url":"https://a/2.jpg"}]}"""))
        assertEquals(null, PlaceSearch.parseImage("""{"meta":{"total_count":0},"documents":[]}"""))
    }
}
