package wtf.log.xmas2019.day.day03

import wtf.log.xmas2019.Day
import java.io.BufferedReader
import java.util.TreeSet
import kotlin.math.absoluteValue

typealias Input = List<List<Day03.Instruction>>

object Day03 : Day<Input, Int, Int> {

    override fun parseInput(reader: BufferedReader): Input = reader
        .lineSequence()
        .map { it.trim() }
        .filterNot { it.isEmpty() }
        .map { it.split(',').map((Instruction)::parse) }
        .toList()

    override fun part1(input: Input): Int {
        val coverageMap = mutableMapOf<Point, MutableSet<Int>>()
        val intersections = TreeSet<Point>()
        input.forEachIndexed { index, line ->
            var position = Point(row = 0, column = 0)
            for (instruction in line) {
                for (newPosition in position.following(instruction)) {
                    position = newPosition
                    val linesAtPosition = coverageMap.getOrPut(position, ::mutableSetOf).apply { add(index) }
                    if (linesAtPosition.size > 1) {
                        intersections += position
                    }
                }
            }
        }
        return intersections.first().manhattanMagnitude
    }

    override fun part2(input: Input): Int? {
        val coverageMap = mutableMapOf<Point, MutableMap<Int, Int>>()
        val intersections = TreeSet<Point>()
        input.forEachIndexed { index, line ->
            var position = Point(row = 0, column = 0)
            var stepCount = 0
            for (instruction in line) {
                for (newPosition in position.following(instruction)) {
                    position = newPosition
                    stepCount += 1
                    val stepCounts = coverageMap.getOrPut(position, ::mutableMapOf).apply { set(index, stepCount) }
                    if (stepCounts.size > 1) {
                        intersections += position
                    }
                }
            }
        }
        return intersections
            .asSequence()
            .map { coverageMap.getValue(it).values.sum() }
            .min()
    }

    data class Point(
        val row: Int = 0,
        val column: Int = 0
    ) : Comparable<Point> {

        val manhattanMagnitude: Int
            get() = row.absoluteValue + column.absoluteValue

        fun following(instruction: Instruction): Sequence<Point> {
            val directionMultiplier = instruction.direction.multiplier
            return (1..instruction.magnitude)
                .asSequence()
                .map { delta ->
                    Point(
                        row = delta * directionMultiplier.row + row,
                        column = delta * directionMultiplier.column + column
                    )
                }
        }

        override fun compareTo(other: Point): Int = this.manhattanMagnitude.compareTo(other.manhattanMagnitude)
    }

    data class Instruction(
        val direction: Direction,
        val magnitude: Int
    ) {

        enum class Direction(val multiplier: Point) {
            LEFT(multiplier = Point(column = -1)),
            UP(multiplier = Point(row = 1)),
            RIGHT(multiplier = Point(column = 1)),
            DOWN(multiplier = Point(row = -1));

            companion object {

                fun fromSymbol(symbol: Char): Direction = when (symbol) {
                    'L' -> LEFT
                    'U' -> UP
                    'R' -> RIGHT
                    'D' -> DOWN
                    else -> throw IllegalArgumentException("Unknown direction: $symbol")
                }
            }
        }

        companion object {

            fun parse(input: String) = Instruction(
                direction = Direction.fromSymbol(input.first()),
                magnitude = input.substring(1).toInt()
            )
        }
    }
}
