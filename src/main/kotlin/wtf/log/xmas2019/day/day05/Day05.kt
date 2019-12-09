package wtf.log.xmas2019.day.day05

import wtf.log.xmas2019.Day
import java.io.BufferedReader
import java.util.ArrayDeque
import java.util.Queue

object Day05 : Day<List<Int>, Int, Int> {

    override fun parseInput(reader: BufferedReader): List<Int> = reader
        .readText()
        .trim()
        .splitToSequence(',')
        .map(String::toInt)
        .toList()

    override fun part1(input: List<Int>): Int {
        val machine = Machine(
            memory = input.toIntArray(),
            input = ArrayDeque<Int>(1).apply { add(1) }
        )
        machine.runToCompletion()
        return machine.output.last()
    }

    override fun part2(input: List<Int>): Int {
        val machine = Machine(
            memory = input.toIntArray(),
            input = ArrayDeque<Int>(1).apply { add(5) }
        )
        machine.runToCompletion()
        return machine.output.last()
    }

    private class Machine(
        private val memory: IntArray,
        private val input: Queue<Int>
    ) {

        private var pc = 0
        private val outputBuilder = mutableListOf<Int>()

        val output: List<Int>
            get() = outputBuilder

        @Suppress("ControlFlowWithEmptyBody")
        fun runToCompletion() {
            while (cycle());
        }

        private fun cycle(): Boolean {
            if (pc < 0) return false
            val instruction = Instruction.from(memory[pc++])
            when (instruction.operation) {
                Operation.ADD -> {
                    val augend1 = getArgument(instruction.modeMask, offset = 0)
                    val augend2 = getArgument(instruction.modeMask, offset = 1)
                    val resultAddress = memory[pc + 2]
                    memory[resultAddress] = augend1 + augend2
                    pc += 3
                }
                Operation.MULTIPLY -> {
                    val multiplicand1 = getArgument(instruction.modeMask, offset = 0)
                    val multiplicand2 = getArgument(instruction.modeMask, offset = 1)
                    val resultAddress = memory[pc + 2]
                    memory[resultAddress] = multiplicand1 * multiplicand2
                    pc += 3
                }
                Operation.INPUT -> {
                    val value = input.remove()
                    val resultAddress = memory[pc++]
                    memory[resultAddress] = value
                }
                Operation.OUTPUT -> {
                    val value = getArgument(instruction.modeMask, offset = 0)
                    outputBuilder += value
                    pc++
                }
                Operation.JUMP_IF_TRUE -> {
                    val subject = getArgument(instruction.modeMask, offset = 0)
                    if (subject != 0) {
                        pc = getArgument(instruction.modeMask, offset = 1)
                    } else {
                        pc += 2
                    }
                }
                Operation.JUMP_IF_FALSE -> {
                    val subject = getArgument(instruction.modeMask, offset = 0)
                    if (subject == 0) {
                        pc = getArgument(instruction.modeMask, offset = 1)
                    } else {
                        pc += 2
                    }
                }
                Operation.LESS_THAN -> {
                    val left = getArgument(instruction.modeMask, offset = 0)
                    val right = getArgument(instruction.modeMask, offset = 1)
                    val resultAddress = memory[pc + 2]
                    memory[resultAddress] = if (left < right) 1 else 0
                    pc += 3
                }
                Operation.EQUALS -> {
                    val left = getArgument(instruction.modeMask, offset = 0)
                    val right = getArgument(instruction.modeMask, offset = 1)
                    val resultAddress = memory[pc + 2]
                    memory[resultAddress] = if (left == right) 1 else 0
                    pc += 3
                }
                Operation.HALT -> {
                    pc = -1
                    return false
                }
            }
            return true
        }

        private fun getArgument(modeMask: List<ParameterMode>, offset: Int): Int {
            val mode = modeMask.getOrElse(offset) { ParameterMode.POSITION }
            val argument = memory[pc + offset]
            return when (mode) {
                ParameterMode.POSITION -> memory[argument]
                ParameterMode.IMMEDIATE -> argument
            }
        }
    }

    private data class Instruction(
        val operation: Operation,
        val modeMask: List<ParameterMode>
    ) {

        companion object {

            fun from(intValue: Int) = Instruction(
                operation = Operation.from(intValue % 100),
                modeMask = (intValue / 100).digits().map((ParameterMode)::get).toList()
            )
        }
    }

    private enum class ParameterMode {
        POSITION,
        IMMEDIATE;

        companion object {

            private val values = values()

            operator fun get(ordinal: Int): ParameterMode = values[ordinal]
        }
    }

    enum class Operation(val code: Int) {
        ADD(code = 1),
        MULTIPLY(code = 2),
        INPUT(code = 3),
        OUTPUT(code = 4),
        JUMP_IF_TRUE(code = 5),
        JUMP_IF_FALSE(code = 6),
        LESS_THAN(code = 7),
        EQUALS(code = 8),
        HALT(code = 99);

        companion object {

            private val values = values()

            fun from(code: Int): Operation = values.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown code: $code")
        }
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
