package com.nisemoe.konata.tools

import com.nisemoe.konata.ReplayEvent
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream
import java.util.*
import kotlin.collections.ArrayList

fun getEvents(replayString: String): ArrayList<ReplayEvent> {
    val decompressedData = decompressData(replayString)
    val replayDataStr = String(decompressedData, Charsets.UTF_8).trimEnd(',')
    return processEvents(replayDataStr)
}

private fun decompressData(replayString: String): ByteArray =
    Base64.getDecoder().decode(replayString).inputStream().use { byteStream ->
        LZMACompressorInputStream(byteStream).readBytes()
    }

internal fun processEvents(replayDataStr: String): ArrayList<ReplayEvent> {
    val eventStrings = replayDataStr.split(",")
    val playData = ArrayList<ReplayEvent>(eventStrings.size)
    eventStrings.forEachIndexed { index, eventStr ->
        val event = createReplayEvent(index, eventStr.split('|'), eventStrings.size)
        event?.let { playData.add(it) }
    }
    return playData
}

private fun createReplayEvent(index: Int, event: List<String>, totalEvents: Int): ReplayEvent? {
    val timeDelta = event[0].toInt()
    val x = event[1].toDouble()
    val y = event[2].toDouble()

    if (timeDelta == -12345 && index == totalEvents - 1) return null
//    if (index < 2 && x == 256.0 && y == -500.0) return null

    return ReplayEvent(timeDelta, x, y)
}