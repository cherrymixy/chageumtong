package com.pure.chageumtong.data

import com.pure.chageumtong.data.navi.GasStationSearch
import com.pure.chageumtong.domain.PlaceCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class GasStationSearchTest {
    @Test
    fun parse_keepsOnlyBrandAndMapsFields() {
        val body = """
            {"meta":{"total_count":2},"documents":[
              {"id":"1","place_name":"HD현대오일뱅크 시청주유소","x":"126.978","y":"37.566","road_address_name":"서울 중구 세종대로 1","address_name":"서울 중구 태평로1가","distance":"1250","phone":""},
              {"id":"2","place_name":"GS칼텍스 종로주유소","x":"126.99","y":"37.57","road_address_name":"","address_name":"서울 종로구","distance":"300"}
            ]}
        """.trimIndent()
        val places = GasStationSearch.parse(body)
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
}
