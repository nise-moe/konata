package com.nisemoe.konata

import com.nisemoe.konata.algorithms.calculateCorrelation
import com.nisemoe.konata.algorithms.calculateDistance
import org.jetbrains.bio.viktor.F64Array


fun compareReplayPair(replay1: String, replay2: String): ReplayPairComparison {
    return compareReplayPair(Replay(replay1), Replay(replay2))
}

fun isValidCoordinate(x: Double, y: Double): Boolean =
    x in 0.0..512.0 && y in 0.0..384.0

/**
    * This function creates a local vector for each replay, removing invalid coordinates and flipping the Y axis if necessary.
    * - We use copies of the original vectors to avoid modifying the original data and maintain integrity.
    * - The y-axis is flipped if the replay has HR enabled and the other doesn't, or vice versa.
 */
fun createLocalVectorPair(
    originalVectorA: F64Array,
    flipVectorA: Boolean,
    originalVectorB: F64Array,
    flipVectorB: Boolean
): Pair<F64Array, F64Array> {
    require(originalVectorA.shape[0] == originalVectorB.shape[0]) { "Vectors must have the same shape." }

    val size = originalVectorA.shape[0]
    val validIndexes = IntArray(size)
    var validCount = 0

    for (i in 0 until size) {
        val xA = originalVectorA[i, 0]
        val yA = originalVectorA[i, 1]

        val xB = originalVectorB[i, 0]
        val yB = originalVectorB[i, 1]

        if (isValidCoordinate(xA, yA) && isValidCoordinate(xB, yB)) {
            validIndexes[validCount++] = i
        }
    }

    val vectorAv2 = F64Array(validCount, 2)
    val vectorBv2 = F64Array(validCount, 2)

    for (i in 0 until validCount) {
        val index = validIndexes[i]

        vectorAv2[i, 0] = originalVectorA[index, 0]
        vectorAv2[i, 1] = if (flipVectorA) 384.0 - originalVectorA[index, 1] else originalVectorA[index, 1]

        vectorBv2[i, 0] = originalVectorB[index, 0]
        vectorBv2[i, 1] = if (flipVectorB) 384.0 - originalVectorB[index, 1] else originalVectorB[index, 1]
    }

    return Pair(vectorAv2, vectorBv2)
}

fun compareReplayPair(replay1: Replay, replay2: Replay): ReplayPairComparison {
    val (longer, shorter) = arrangeReplaysByLength(replay1, replay2)
    val interpolatedShorterData = linearInterpolate(shorter, longer)

    val (localVectorA, localVectorB) = createLocalVectorPair(
        interpolatedShorterData,
        shorter.hasHR() && !longer.hasHR(),
        longer.vector,
        !shorter.hasHR() && longer.hasHR()
    )

    require(localVectorA.shape[0] == localVectorB.shape[0]) { "Datasets must have the same size." }
    return ReplayPairComparison(
        similarity = calculateDistance(localVectorA, localVectorB),
        correlation = calculateCorrelation(localVectorA, localVectorB)
    )
}

private fun arrangeReplaysByLength(replay1: Replay, replay2: Replay): Pair<Replay, Replay> =
    if (replay1.vector.shape[0] > replay2.vector.shape[0]) replay1 to replay2 else replay2 to replay1

/**
 * Performs linear interpolation between two vectors of different sizes.
 * We assume that vectorA is the smaller one, and vectorB is the larger one.
 *
 * The (x,y) data in vectorA will be "stretched" to match the time points in vectorB.
 *
 * The length of the returned vector will be the same as vectorB.
 *
 * @param replayA The replay with fewer elements, used as the base for interpolation.
 * @param replayB The replay with more elements, where interpolation targets are found.
 * @return A vector with the same shape as vectorB, with the interpolated data.
 */
private fun linearInterpolate(replayA: Replay, replayB: Replay): F64Array {
    val returnVector = F64Array(replayB.vector.shape[0], 2)
    val lIndex = replayA.axis.lastIndex
    var maxLower = 0

    for (indexB in 0..<replayB.vector.shape[0]) {
        val xi = replayB.vector[indexB, 2]
        val index = replayA.axis.binarySearch(xi, fromIndex = maxLower)
        val insertionPoint = if (index >= 0) index else -(index + 1)
        val lower = if (index >= 0) index else insertionPoint.coerceIn(1, lIndex + 1) - 1
        val upper = if (index >= 0) index else insertionPoint.coerceIn(0, lIndex)
        maxLower = lower
        if (lower == upper) {
            returnVector[indexB, 0] = replayA.vector[lower, 0]
            returnVector[indexB, 1] = replayA.vector[lower, 1]
        } else {
            val t = (xi - replayA.vector[lower, 2]) / (replayA.vector[upper, 2] - replayA.vector[lower, 2])
            returnVector[indexB, 0] = replayA.vector[lower, 0] + t * (replayA.vector[upper, 0] - replayA.vector[lower, 0])
            returnVector[indexB, 1] = replayA.vector[lower, 1] + t * (replayA.vector[upper, 1] - replayA.vector[lower, 1])
        }
    }
    return returnVector
}