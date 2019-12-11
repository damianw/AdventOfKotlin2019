package wtf.log.xmas2019.day.day10

import org.apache.commons.math3.fraction.Fraction
import wtf.log.xmas2019.Day
import java.io.BufferedReader
import java.util.NavigableSet
import java.util.TreeSet
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.atan2

object Day10 : Day<Day10.Grid, Int, Int> {

    override fun parseInput(reader: BufferedReader): Grid = reader
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toList()
        .let((Grid)::parse)

    override fun part1(input: Grid): Int = input.findBest().second.size

    override fun part2(input: Grid): Int? {
        val eliminations = mutableListOf<Point>()
        val station: Point
        var detections: NavigableSet<Detection>
        input.findBest().let { (point, set) ->
            station = point
            detections = set
        }
        outer@ while (detections.isNotEmpty()) {
            for ((point) in detections) {
                if (eliminations.size >= 200) break@outer
                input[point] = false
                eliminations += point
            }
            detections = input.detectionsFrom(station)
        }
        val winner = eliminations[199]
        return (winner.x * 100) + winner.y
    }

    data class Detection(val point: Point, val angle: Double) : Comparable<Detection> {

        override fun compareTo(other: Detection): Int = angle.compareTo(other.angle)
    }

    data class Point(val x: Int = 0, val y: Int = 0) {

        fun pointsAlongLineTo(other: Point): Sequence<Point> = when (val deltaX = other.x - x) {
            0 -> {
                val changeY = (other.y - y).absoluteValue
                val range = if (other.y > y) y..other.y else other.y..y
                range
                    .asSequence()
                    .map { copy(y = it) }
                    .drop(1)
                    .take(changeY - 1)
            }
            else -> {
                val slope = Fraction(other.y - y, deltaX)
                val startingPoint: Point
                val endingPoint: Point
                if (deltaX > 0) {
                    startingPoint = this@Point
                    endingPoint = other
                } else {
                    startingPoint = other
                    endingPoint = this@Point
                }
                sequence {
                    var currentPoint = startingPoint
                    while (currentPoint != endingPoint) {
                        currentPoint = currentPoint.copy(
                            x = currentPoint.x + slope.denominator,
                            y = currentPoint.y + slope.numerator
                        )
                        yield(currentPoint)
                    }
                }.takeWhile { it != endingPoint }
            }
        }
    }

    class Grid(private val asteroids: BooleanArray, val width: Int) {

        val height: Int
            get() = asteroids.size / width

        fun detectionsFrom(point: Point): NavigableSet<Detection> = occupiedCoordinates()
            .filter { other ->
                other != point && point.pointsAlongLineTo(other).none(::get)
            }
            .map { other ->
                // Purposefully invert Y because our coordinate system is upside-down relative to atan2
                val deltaY = (point.y - other.y).toDouble()
                val deltaX = (other.x - point.x).toDouble()
                Detection(other, angle = (atan2(deltaX, deltaY) + (2 * PI)) % (2 * PI))
            }
            .toCollection(TreeSet())

        fun findBest(): Pair<Point, NavigableSet<Detection>> = occupiedCoordinates()
            .map { it to detectionsFrom(it) }
            .maxBy { it.second.size }!!

        private fun occupiedCoordinates(): Sequence<Point> = asteroids
            .asSequence()
            .mapIndexed { index, isOccupied -> if (isOccupied) Point(x = index % width, y = index / width) else null }
            .filterNotNull()

        operator fun get(point: Point) = get(point.x, point.y)

        operator fun get(x: Int, y: Int) = asteroids[y * width + x]

        operator fun set(point: Point, value: Boolean) = set(point.x, point.y, value)

        operator fun set(x: Int, y: Int, value: Boolean) {
            asteroids[y * width + x] = value
        }

        override fun toString(): String = buildString {
            append('┌')
            repeat(width) {
                append('─')
            }
            append("┐\n")
            repeat(height) { y ->
                append('│')
                repeat(width) { x ->
                    append(if (get(x, y)) ASTEROID else EMPTY)
                }
                append("│\n")
            }
            append('└')
            repeat(width) {
                append('─')
            }
            append('┘')
        }

        companion object {

            private const val ASTEROID = '#'
            private const val EMPTY = '.'

            fun parse(lines: List<String>): Grid {
                require(lines.isNotEmpty())
                val width = lines.first().length
                require(lines.all { it.length == width })
                val asteroids = BooleanArray(width * lines.size) { index ->
                    when (val character = lines[index / width][index % width]) {
                        ASTEROID -> true
                        EMPTY -> false
                        else -> throw IllegalArgumentException("Unknown symbol: $character")
                    }
                }
                return Grid(asteroids, width)
            }
        }
    }
}
