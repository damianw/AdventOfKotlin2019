package wtf.log.xmas2019.day.day01

import wtf.log.xmas2019.Day
import java.io.BufferedReader

object Day01 : Day<List<Int>, Int, Int> {

    override fun parseInput(reader: BufferedReader): List<Int> = reader
        .lineSequence()
        .map(String::toInt)
        .toList()

    override fun part1(input: List<Int>): Int = input.sumBy { mass -> (mass / 3) - 2 }

    override fun part2(input: List<Int>): Int = input.sumBy { mass ->
        generateSequence(mass) { part -> (part / 3) - 2 }
            .drop(1)
            .takeWhile { it > 0 }
            .sum()
    }
}
