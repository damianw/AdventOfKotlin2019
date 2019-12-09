package wtf.log.xmas2019.day.day02

import wtf.log.xmas2019.Day
import java.io.BufferedReader

object Day02 : Day<List<Int>, Int, Int> {

    override fun parseInput(reader: BufferedReader): List<Int> = reader
        .readText()
        .trim()
        .splitToSequence(',')
        .map(String::toInt)
        .toList()

    override fun part1(input: List<Int>): Int = execute(input, noun = 12, verb = 2)

    override fun part2(input: List<Int>): Int {
        for (noun in 0..99) {
            for (verb in 0..99) {
                if (execute(input, noun, verb) == 19690720) {
                    return 100 * noun + verb
                }
            }
        }
        throw NoSuchElementException("No appropriate noun+verb combination for program: $input")
    }

    private fun execute(program: List<Int>, noun: Int, verb: Int): Int {
        var pc = 0
        val memory = program.toIntArray()
        memory[1] = noun
        memory[2] = verb
        loop@ while (true) {
            when (val opcode = memory[pc]) {
                1 -> {
                    val augend1 = memory[memory[pc + 1]]
                    val augend2 = memory[memory[pc + 2]]
                    val resultAddress = memory[pc + 3]
                    memory[resultAddress] = augend1 + augend2
                }
                2 -> {
                    val multiplicand1 = memory[memory[pc + 1]]
                    val multiplicand2 = memory[memory[pc + 2]]
                    val resultAddress = memory[pc + 3]
                    memory[resultAddress] = multiplicand1 * multiplicand2
                }
                99 -> break@loop
                else -> throw UnsupportedOperationException("Unknown opcode: $opcode")
            }
            pc += 4
        }
        return memory[0]
    }
}
