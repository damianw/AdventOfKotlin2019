package wtf.log.xmas2019.day.day04

import wtf.log.xmas2019.Day
import java.io.BufferedReader

object Day04 : Day<IntRange, Int, Int> {

    override fun parseInput(reader: BufferedReader): IntRange = reader
        .readText()
        .trim()
        .split('-')
        .let { (start, endInclusive) ->
            start.toInt()..endInclusive.toInt()
        }

    override fun part1(input: IntRange): Int = input.count { candidate ->
        val digitPairs = candidate.digits().zipWithNext()
        val isIncreasing = digitPairs.all { (next, previous) -> next >= previous }
        isIncreasing && digitPairs.any { (next, previous) -> next == previous }
    }

    override fun part2(input: IntRange): Int = input.count { candidate ->
        val digits = candidate.digits()
        val isIncreasing = digits.zipWithNext().all { (next, previous) -> next >= previous }
        isIncreasing && digits.partialWindowed().any { (next, left, right, previous) ->
            left == right && left != next && right != previous
        }
    }

    private fun <T : Any> Sequence<T>.partialWindowed(): Sequence<Window<T>> {
        val padding = sequenceOf(null)
        return (padding + this + padding).windowed(size = 4) { elements ->
            Window(
                start = elements[0],
                left = elements[1]!!,
                right = elements[2]!!,
                end = elements[3]
            )
        }
    }

    private data class Window<T : Any>(
        val start: T?,
        val left: T,
        val right: T,
        val end: T?
    ) {

        override fun toString(): String = "(${start ?: "_"}, $left, $right, ${end ?: "_"})"
    }

    /**
     * Returns digits of the receiver (using [radix]) in little-endian (ascending order).
     */
    private fun Int.digits(radix: Int = 10): Sequence<Int> = sequence {
        var value = this@digits
        do {
            yield(value % radix)
            value /= radix
        } while (value != 0)
    }
}
