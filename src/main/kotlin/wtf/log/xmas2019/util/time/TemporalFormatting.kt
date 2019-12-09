package wtf.log.xmas2019.util.time

import java.time.Duration
import java.time.temporal.ChronoUnit

private val formatChronoUnits = ChronoUnit.values()
    .takeWhile { !it.isDurationEstimated }
    .reversed()

fun Duration.toPrettyFormat(): String {
    val totalNanos = toNanos()
    return formatChronoUnits
        .asSequence()
        .dropWhile { totalNanos / it.duration.toNanos() == 0L }
        .fold(Pair(totalNanos, listOf<String>())) { (nanos, parts), unit ->
            val durationNanos = unit.duration.toNanos()
            val count = nanos / durationNanos
            val remainderNanos = nanos % durationNanos
            remainderNanos to (parts + "$count ${unit.toString().toLowerCase()}")
        }
        .second
        .joinToString()
}
