package com.nisemoe.konata.tools

import com.nisemoe.konata.ReplayEvent
import org.jetbrains.bio.viktor.F64Array

fun processReplayData(events: ArrayList<ReplayEvent>): F64Array {
    if (events.isEmpty()) throw IllegalArgumentException("This replay's replay data was empty. It indicates a misbehaved replay.")

    if (events.first().timeDelta == 0 && events.size > 1) events.removeAt(0)

    val pEvents = ArrayList<Triple<Double, Double, Double>>()

    var cumulativeTimeDelta = events.first().timeDelta
    var highestTimeDelta = Double.NEGATIVE_INFINITY
    var lastPositiveFrame: ReplayEvent? = null

    var wasInNegativeSection = false
    val lastPositiveFrameData = mutableListOf<Pair<Double, Pair<Double, Double>>>()

    events.drop(1).forEachIndexed { index, currentFrame ->
        val previousCumulativeTime = cumulativeTimeDelta
        cumulativeTimeDelta += currentFrame.timeDelta

        highestTimeDelta = maxOf(highestTimeDelta, cumulativeTimeDelta.toDouble())

        val isInNegativeSection = cumulativeTimeDelta < highestTimeDelta
        if (isInNegativeSection) {
            if (!wasInNegativeSection) {
                lastPositiveFrame = if (index > 0) events[index - 1] else null
            }
        } else {
            if (wasInNegativeSection && lastPositiveFrame != null) {
                val lastPositiveTime = lastPositiveFrameData.lastOrNull()?.first ?: previousCumulativeTime.toDouble()
                val ratio = (lastPositiveTime - previousCumulativeTime) / (cumulativeTimeDelta - previousCumulativeTime).toDouble()

                val interpolatedX = lastPositiveFrame!!.x + ratio * (currentFrame.x - lastPositiveFrame!!.x)
                val interpolatedY = lastPositiveFrame!!.y + ratio * (currentFrame.y - lastPositiveFrame!!.y)

                pEvents.add(Triple(interpolatedX, interpolatedY, lastPositiveTime))
            }
            wasInNegativeSection = false
        }

        wasInNegativeSection = isInNegativeSection

        if (!isInNegativeSection)
            pEvents.add(Triple(currentFrame.x, currentFrame.y, cumulativeTimeDelta.toDouble()))

        if (!isInNegativeSection)
            lastPositiveFrameData.add(Pair(cumulativeTimeDelta.toDouble(), Pair(currentFrame.x, currentFrame.y)))
    }

    val pEventsUnique = pEvents.distinctBy { it.third }

    return F64Array(pEventsUnique.size, 3) { index, dim ->
        when (dim) {
            0 -> pEventsUnique[index].first
            1 -> pEventsUnique[index].second
            else -> pEventsUnique[index].third
        }
    }
}