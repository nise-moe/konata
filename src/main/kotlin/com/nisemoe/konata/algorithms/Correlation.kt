package com.nisemoe.konata.algorithms

import kotlinx.coroutines.*
import org.apache.commons.math3.stat.descriptive.rank.Median
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer.transformInPlace
import org.apache.commons.math3.transform.TransformType
import org.jetbrains.bio.viktor.F64Array
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

/**
 * Calculates the cross-correlation of two 1d arrays.
 * Parameters:
 * - arrayA: The first input array.
 * - arrayB: The second input array.
 * Returns:
 * - Discrete linear cross-correlation of arrayA and arrayB.
 */
fun getCrossCorrelation(arrayA: F64Array, arrayB: F64Array): F64Array {
    val n = arrayA.shape[0] + arrayB.shape[0] - 1
    val size = 2.0.pow(ceil(log2(n.toDouble()))).toInt()

    val transformArrayA = Array(2) { DoubleArray(size) }
    val transformArrayB = Array(2) { DoubleArray(size) }

    for (i in 0 until size) {
        if (i < arrayA.shape[0]) {
            transformArrayA[0][i] = arrayA[i, 0]
            transformArrayA[1][i] = arrayA[i, 1]
        }
        if (i < arrayB.shape[0]) {
            transformArrayB[0][i] = arrayB[arrayB.shape[0] - i - 1, 0]
            transformArrayB[1][i] = -arrayB[arrayB.shape[0] - i - 1, 1]
        }
    }

    transformInPlace(transformArrayA, DftNormalization.STANDARD, TransformType.FORWARD)
    transformInPlace(transformArrayB, DftNormalization.STANDARD, TransformType.FORWARD)

    for (i in 0 until size) {
        val a = transformArrayA[0][i]
        val b = transformArrayA[1][i]
        val c = transformArrayB[0][i]
        val d = transformArrayB[1][i]

        transformArrayA[0][i] = a * c - b * d
        transformArrayA[1][i] = a * d + b * c
    }

    transformInPlace(transformArrayA, DftNormalization.STANDARD, TransformType.INVERSE)

    return F64Array(n) { index ->
        if (index < size) transformArrayA[0][index] else 0.0
    }
}

fun calculateCorrelation(vectorA: F64Array, vectorB: F64Array): Double = runBlocking {
    val correlations = mutableListOf<Deferred<Double>>()
    val i1 = vectorA.shape[0] / 5

    for (i in 0 until 5) {
        val startIdx = i * i1
        val endIdx = if (i < 4) startIdx + i1 else vectorA.shape[0]

        correlations.add(async(Dispatchers.Default) {
            val vectorChunkA = vectorA.slice(startIdx, endIdx).copy()
            val vectorChunkB = vectorB.slice(startIdx, endIdx).copy()

            vectorChunkA -= vectorChunkA.mean()
            vectorChunkB -= vectorChunkB.mean()

            val norm = vectorChunkA.flatten().sd() * vectorChunkB.flatten().sd() * (vectorChunkA.shape[0] * 2)
            val correlation = getCrossCorrelation(
                vectorChunkA,
                vectorChunkB
            )
            correlation /= norm
            correlation.max()
        })
    }

    Median().evaluate(correlations.awaitAll().toDoubleArray())
}