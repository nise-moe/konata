# konata

>osu! utility lib in kotlin for fast replay comparison with multithreading support

This module has the specific purpose of **high-throughput** replay comparison, and only works with replay data as supplied by the osu!api; it does not work with .osr files.

[circleguard](https://github.com/circleguard/circleguard) is a better tool if you are looking for a more complete solution, as it has a GUI and supports .osr files.

this module was built with a narrow task in mind, and I do not have plans to implement more features (especially if circleguard already covers them)

# Usage

### Replay data class

`Replay` is the main data class you'll be throwing around. The only required field is the replay data (verbatim as fetched by the osu!api) in string format.

You can also pass additional parameters:

| parameter | type | required?                    | notes                                                                                                       |
|-----------|------|------------------------------|-------------------------------------------------------------------------------------------------------------|
| id        | Long | not for pairs, yes for sets* | used to find the replay in the output, does NOT have to match osu!api, it can be any identifier you'd like. |
| mods      | Int  | no (defaults to NoMod)       | exact value as fetched by the osu!api, it's used to flip the replay y-axis when HR is enabled.              |

*You are forced to set the id when using the replay in a set comparison, as it is the identifier that will allow you to match the input to the results.

Example:

```kotlin
// Simplest replay
val replay: Replay = Replay(replayString)

// A NoMod replay with id 1
val replay: Replay = Replay(replayString, id = 1, mods = 0)

// A HDHR (24) replay with id 2
val replay: Replay = Replay(replayString, id = 2, mods = 24)
```

### Replay pairs (2 replays)

The replay strings must be exactly as provided by the osu!api replay endpoint.

The following code calculates the similarity ratio and correlation ratio between two replays, without specifying any mods.

```kotlin
// Compare using objects
val replay1: Replay = Replay(replay1String)
val replay2: Replay = Replay(replay2String)

val result: ReplayPairComparison = compareReplayPair(replay1, replay2)
println(result.similarity) // 20.365197244184895
println(result.correlation) // 0.9770151700235653

// You can also pass the replay data directly as strings
val similarity: ReplayPairComparison = compareReplayPair(replay1String, replay2String)
println(result.similarity) // 20.365197244184895
println(result.correlation) // 0.9770151700235653
```

### Replay sets (n replays)

If we decide to pass a list of replays, there will be optimizations such as multi-threading involved, which can speed up the calculations.

When comparing sets, you *must* set the replay id (it does not have to match the osu! replay id), as it is the identifier that will
allow you to match the input to the results.

```kotlin
// Compare using objects
val replays: Array<Replay> = arrayOf(
    Replay("...", id = 1),
    Replay("...", id = 2)
)

val result: List<ReplaySetComparison> = compareReplaySet(replays)
println(result[0].replay1Id) // 1
println(result[0].replay2Id) // 2
println(result[0].similarity) // 155.20954003316618
println(result[0].correlation) // 0.9859198745055805
```

By default, the `compareReplaySet` method will default to using as many threads as there are cores on your system.
You can change this behaviour by manually passing an amount of cores to use:

```kotlin
compareReplaySet(replays, numThreads=4)
```

# Benchmarks

### Performance

On my development machine (5900X), the following benchmarks were obtained.

I processed 10 batches of 100 replays each. The min/max/avg time refer to single batches.

|             | version     | min  | max  | avg  | total | pairs/second |
|-------------|-------------|------|------|------|-------|--------------|
|             | v20240211   | 3.1s | 4.2s | 3.3s | 32.7s | 1501/s       |
|             | v20240211v2 | 2.5s | 3.7s | 2.7s | 26.7s | 1843/s       |
| **current** | v20240211v3 | 1.1s | 2.1s | 1.3s | 13.0s | 3789/s       |

### Accuracy (compared to Circleguard)

>as of the last version, konata and circleguard give the same results, with a neglibile margin of error.

After selecting a random dataset of ~50,000 osu!std replays for different beatmaps, I compared the results from konata to circleguard, using the latter as the ground truth.

| metric        | avg. delta | std. dev.  | median    | min       | max       |
|---------------|------------|------------|-----------|-----------|-----------|
| `SIMILARITY`  | 0          | 0.000033   | 0         | -0.005373 | 0.007381  |
| `CORRELATION` | -0.000643  | 0.001342   | -0.000433 | -0.041833 | 0.026300  |
