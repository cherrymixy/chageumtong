package com.pure.chageumtong.ui

import kotlin.math.cos
import kotlin.math.hypot

/**
 * [위도, 경도] 경로 위에서 길이 비율(0~1)로 위치를 찾는다. 주행 km ÷ 총 주행거리 = 비율.
 * 거리는 서울 범위 짧은 구간이라 평면 근사로 충분 (비율만 쓰므로 단위도 상관없음).
 */
class RoutePath(private val pts: List<List<Double>>) {
    private val cum: DoubleArray = DoubleArray(pts.size).also { c ->
        for (i in 1 until pts.size) c[i] = c[i - 1] + dist(pts[i - 1], pts[i])
    }
    private val total = cum.lastOrNull() ?: 0.0

    /** 비율 f 위치의 점 [위도, 경도] */
    fun at(f: Double): List<Double> {
        if (pts.size < 2 || total == 0.0) return pts.firstOrNull() ?: listOf(0.0, 0.0)
        val target = f.coerceIn(0.0, 1.0) * total
        val i = cum.indexOfFirst { it >= target }.coerceIn(1, pts.lastIndex)
        val seg = cum[i] - cum[i - 1]
        val t = if (seg == 0.0) 0.0 else (target - cum[i - 1]) / seg
        return listOf(lerp(pts[i - 1][0], pts[i][0], t), lerp(pts[i - 1][1], pts[i][1], t))
    }

    /** 비율 f0~f1 사이 경로 (양 끝은 보간한 점, 가운데는 원래 꼭짓점) */
    fun slice(f0: Double, f1: Double): List<List<Double>> {
        val a = f0.coerceIn(0.0, 1.0)
        val b = f1.coerceIn(0.0, 1.0)
        if (b <= a || total == 0.0) return emptyList()
        val inside = pts.indices.filter { cum[it] > a * total && cum[it] < b * total }.map { pts[it] }
        return listOf(at(a)) + inside + listOf(at(b))
    }

    private fun lerp(x: Double, y: Double, t: Double) = x + (y - x) * t
    private fun dist(p: List<Double>, q: List<Double>) =
        hypot(q[0] - p[0], (q[1] - p[1]) * cos(Math.toRadians(p[0])))
}
