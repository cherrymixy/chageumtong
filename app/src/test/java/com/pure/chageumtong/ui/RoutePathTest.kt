package com.pure.chageumtong.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePathTest {
    // 적도 위 같은 길이 두 구간: (0,0) → (0,1) → (0,2)
    private val r = RoutePath(listOf(listOf(0.0, 0.0), listOf(0.0, 1.0), listOf(0.0, 2.0)))

    @Test fun `비율 위치`() {
        assertEquals(listOf(0.0, 0.0), r.at(0.0))
        assertEquals(1.0, r.at(0.5)[1], 1e-9)
        assertEquals(1.5, r.at(0.75)[1], 1e-9)
        assertEquals(2.0, r.at(1.0)[1], 1e-9)
        assertEquals(2.0, r.at(1.7)[1], 1e-9) // 범위 밖은 끝으로
    }

    @Test fun `구간 자르기는 양끝 보간 + 가운데 꼭짓점`() {
        val s = r.slice(0.25, 0.75)
        assertEquals(3, s.size)
        assertEquals(0.5, s.first()[1], 1e-9)
        assertEquals(1.0, s[1][1], 1e-9)
        assertEquals(1.5, s.last()[1], 1e-9)
    }

    @Test fun `빈 구간`() {
        assertTrue(r.slice(0.6, 0.4).isEmpty())
        assertEquals(listOf(0.0, 0.0), RoutePath(emptyList()).at(0.5))
    }
}
