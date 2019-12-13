package wtf.log.xmas2019.day.day12

import wtf.log.xmas2019.Day
import java.io.BufferedReader
import kotlin.math.absoluteValue
import kotlin.math.sign

object Day12 : Day<List<Day12.Moon>, Int, Int> {

    override fun parseInput(reader: BufferedReader): List<Moon> = reader
        .lineSequence()
        .mapIndexed { index, line ->
            Moon(
                id = index,
                position = Vec3.parse(line),
                velocity = Vec3(0, 0, 0)
            )
        }
        .toList()

    override fun part1(input: List<Moon>): Int = input
        .timeline()
        .take(1001)
        .last()
        .sumBy { it.totalEnergy }

    override fun part2(input: List<Moon>): Int {
        return 0
    }

    private fun List<Moon>.timeline(): Sequence<List<Moon>> = generateSequence(this) { moons ->
        moons.map { it.timeStep(moons) }
    }

    data class Moon(
        val id: Int,
        val position: Vec3,
        val velocity: Vec3
    ) {

        val potentialEnergy: Int
            get() = position.toEnergy()

        val kineticEnergy: Int
            get() = velocity.toEnergy()

        val totalEnergy: Int
            get() = potentialEnergy * kineticEnergy

        fun timeStep(moons: List<Moon>): Moon {
            val newVelocity = velocity.copy(
                x = moons.velocityDelta(Vec3::x),
                y = moons.velocityDelta(Vec3::y),
                z = moons.velocityDelta(Vec3::z)
            )
            return copy(
                position = position.copy(
                    x = position.x + newVelocity.x,
                    y = position.y + newVelocity.y,
                    z = position.z + newVelocity.z
                ),
                velocity = newVelocity
            )
        }

        private fun Vec3.toEnergy() = x.absoluteValue + y.absoluteValue + z.absoluteValue

        private inline fun List<Moon>.velocityDelta(component: Vec3.() -> Int): Int = velocity.component() +
            sumBy { other ->
                (other.position.component() - position.component()).sign
            }
    }

    data class Vec3(val x: Int, val y: Int, val z: Int) {

        companion object {

            private val PATTERN = Regex("""<x=(-?\d+), y=(-?\d+), z=(-?\d+)>""")

            fun parse(input: String): Vec3 = PATTERN
                .matchEntire(input)
                ?.destructured
                ?.let { (x, y, z) ->
                    Vec3(x = x.toInt(), y = y.toInt(), z = z.toInt())
                }
                ?: throw IllegalArgumentException("Malformed vector: $input")
        }
    }
}
