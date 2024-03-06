package com.nisemoe.konata

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

fun <T> Array<T>.combinations(): Sequence<Pair<T, T>> = sequence {
    for (i in indices) {
        for (j in i + 1 until this@combinations.size) {
            yield(this@combinations[i] to this@combinations[j])
        }
    }
}

fun removeDuplicateReplays(replaySet: Array<Replay>): Array<Replay> {
    val uniqueReplays = replaySet.toList().distinctBy { it.id }
    return uniqueReplays.toTypedArray()
}

fun compareReplaySet(replaySet: Array<Replay>,
                     numThreads: Int = Runtime.getRuntime().availableProcessors(),
): List<ReplaySetComparison> = runBlocking {
    if(replaySet.any { it.id == null })
        throw IllegalArgumentException("All replays must have an ID when calling compareReplaySet!")

    val replaySetUnique = removeDuplicateReplays(replaySet)

    val dispatcher = Executors
        .newFixedThreadPool(numThreads)
        .asCoroutineDispatcher()

    val result = mutableListOf<ReplaySetComparison>()

    coroutineScope {
        replaySetUnique.combinations().forEach { (replay1, replay2) ->
            launch(dispatcher) {
                val comparisonResult = compareReplayPair(replay1, replay2)
                result.add(
                    ReplaySetComparison(
                        replay1Id = replay1.id!!,
                        replay1Mods = replay1.mods,

                        replay2Id = replay2.id!!,
                        replay2Mods = replay2.mods,

                        similarity = comparisonResult.similarity,
                        correlation = comparisonResult.correlation
                    )
                )
            }
        }
    }

    dispatcher.close()
    return@runBlocking result
}

fun compareSingleReplayWithSet(
    singleReplay: Replay,
    replaySet: Array<Replay>,
    numThreads: Int = Runtime.getRuntime().availableProcessors()
): List<ReplaySetComparison> = runBlocking {
    if(replaySet.any { it.id == null })
        throw IllegalArgumentException("All replays must have an ID when calling compareSingleReplayWithSet!")

    val dispatcher = Executors
        .newFixedThreadPool(numThreads)
        .asCoroutineDispatcher()

    val result = mutableListOf<ReplaySetComparison>()

    coroutineScope {
        replaySet.forEach { replay ->
            launch(dispatcher) {
                val comparisonResult = compareReplayPair(singleReplay, replay)
                result.add(
                    ReplaySetComparison(
                        replay1Id = singleReplay.id!!,
                        replay1Mods = singleReplay.mods,

                        replay2Id = replay.id!!,
                        replay2Mods = replay.mods,

                        similarity = comparisonResult.similarity,
                        correlation = comparisonResult.correlation
                    )
                )
            }
        }
    }

    dispatcher.close()
    return@runBlocking result
}