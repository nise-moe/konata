package com.nisemoe.konata.algorithms

import org.jetbrains.bio.viktor.F64Array
import org.jetbrains.bio.viktor._I

/**
 * Calculates the Euclidean distance between two vectors. The vectors *have* to be the same shape.
 *
 * It can look weird because the F64Array library (viktor) doesn't support operations such as .pow()
 * so I've had to work with what we had.
 */
fun calculateDistance(vectorA: F64Array, vectorB: F64Array): Double {
    require(vectorA.shape[0] == vectorB.shape[0]) { "Vectors must have the same shape." }

    val difference = vectorA - vectorB
    difference.timesAssign(difference)

    val intermediateResults = F64Array(vectorB.shape[0])

    val sumOfSquaresX = difference.V[_I, 0]
    val sumOfSquaresY = difference.V[_I, 1]
    intermediateResults += (sumOfSquaresX + sumOfSquaresY)

    intermediateResults.logInPlace()
    intermediateResults *= 0.5
    intermediateResults.expInPlace()

    return intermediateResults.mean()
}
