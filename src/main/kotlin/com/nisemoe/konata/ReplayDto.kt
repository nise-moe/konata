package com.nisemoe.konata

data class ReplayPairComparison(
    val similarity: Double,
    val correlation: Double
)

data class ReplaySetComparison(
    val replay1Id: Long,
    val replay1Mods: Int,

    val replay2Id: Long,
    val replay2Mods: Int,

    val similarity: Double,
    val correlation: Double
)

data class ReplayEvent(
    val timeDelta: Int,
    val x: Double,
    val y: Double
)