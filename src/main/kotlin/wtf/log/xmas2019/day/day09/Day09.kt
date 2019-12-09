package wtf.log.xmas2019.day.day09

import wtf.log.xmas2019.Day
import java.io.BufferedReader
import java.util.ArrayDeque
import java.util.Queue

object Day09 : Day<List<Long>, Long, Long> {

    override fun parseInput(reader: BufferedReader): List<Long> = reader
        .readText()
        .trim()
        .splitToSequence(',')
        .map(String::toLong)
        .toList()

    override fun part1(input: List<Long>): Long {
        val machine = Machine(input)
        val output = machine.runToCompletion(ArrayDeque<Long>().apply { addFirst(1L) })
        return output.single()
    }

    override fun part2(input: List<Long>): Long {
        val machine = Machine(input)
        val output = machine.runToCompletion(ArrayDeque<Long>().apply { addFirst(2L) })
        return output.single()
    }

    private class Machine(
        private val program: List<Long>
    ) {

        private val memory = mutableMapOf<Long, Long>()
        private var relativeBase = 0L
        private var pc = 0L

        @Suppress("ControlFlowWithEmptyBody")
        fun runToCompletion(input: Queue<Long>): List<Long> {
            pc = 0
            memory.clear()
            program.forEachIndexed { index, value ->
                memory[index.toLong()] = value
            }
            val output = mutableListOf<Long>()
            while (cycle(input, output));
            return output
        }

        private fun cycle(input: Queue<Long>, output: MutableList<Long>): Boolean {
            if (pc < 0) return false
            val instruction = Instruction.from(memory.getOrElse(pc++) { 0L })
            when (instruction.operation) {
                Operation.ADD -> {
                    val augend1 = getArgument(instruction.modeMask, offset = 0)
                    val augend2 = getArgument(instruction.modeMask, offset = 1)
                    writeResult(instruction.modeMask, offset = 2, value = augend1 + augend2)
                    pc += 3
                }
                Operation.MULTIPLY -> {
                    val multiplicand1 = getArgument(instruction.modeMask, offset = 0)
                    val multiplicand2 = getArgument(instruction.modeMask, offset = 1)
                    writeResult(instruction.modeMask, offset = 2, value =  multiplicand1 * multiplicand2)
                    pc += 3
                }
                Operation.INPUT -> {
                    val value = input.remove()
                    writeResult(instruction.modeMask, offset = 0, value = value)
                    pc++
                }
                Operation.OUTPUT -> {
                    val value = getArgument(instruction.modeMask, offset = 0)
                    output += value
                    pc++
                }
                Operation.JUMP_IF_TRUE -> {
                    val subject = getArgument(instruction.modeMask, offset = 0)
                    if (subject != 0L) {
                        pc = getArgument(instruction.modeMask, offset = 1)
                    } else {
                        pc += 2
                    }
                }
                Operation.JUMP_IF_FALSE -> {
                    val subject = getArgument(instruction.modeMask, offset = 0)
                    if (subject == 0L) {
                        pc = getArgument(instruction.modeMask, offset = 1)
                    } else {
                        pc += 2
                    }
                }
                Operation.LESS_THAN -> {
                    val left = getArgument(instruction.modeMask, offset = 0)
                    val right = getArgument(instruction.modeMask, offset = 1)
                    writeResult(instruction.modeMask, offset = 2, value = if (left < right) 1L else 0L)
                    pc += 3
                }
                Operation.EQUALS -> {
                    val left = getArgument(instruction.modeMask, offset = 0)
                    val right = getArgument(instruction.modeMask, offset = 1)
                    writeResult(instruction.modeMask, offset = 2, value = if (left == right) 1L else 0L)
                    pc += 3
                }
                Operation.RELATIVE_BASE_OFFSET -> {
                    val offset = getArgument(instruction.modeMask, offset = 0)
                    relativeBase += offset
                    pc++
                }
                Operation.HALT -> {
                    pc = -1
                    return false
                }
            }
            return true
        }

        private fun getArgument(modeMask: List<ParameterMode>, offset: Long): Long {
            val mode = modeMask.getOrElse(offset.toInt()) { ParameterMode.POSITION }
            val argument = memory.getOrElse(pc + offset) { 0L }
            return when (mode) {
                ParameterMode.POSITION -> memory.getOrElse(argument) { 0L }
                ParameterMode.IMMEDIATE -> argument
                ParameterMode.RELATIVE -> memory.getOrElse(relativeBase + argument) { 0L }
            }
        }

        private fun writeResult(modeMask: List<ParameterMode>, offset: Long, value: Long) {
            val mode = modeMask.getOrElse(offset.toInt()) { ParameterMode.POSITION }
            val argument = memory.getOrElse(pc + offset) { 0L }
            when (mode) {
                ParameterMode.POSITION -> memory[argument] = value
                ParameterMode.IMMEDIATE -> throw UnsupportedOperationException("Cannot write in immediate mode")
                ParameterMode.RELATIVE -> memory[relativeBase + argument] = value
            }
        }
    }

    private data class Instruction(
        val operation: Operation,
        val modeMask: List<ParameterMode>
    ) {

        companion object {

            fun from(intValue: Long) = Instruction(
                operation = Operation.from(intValue % 100),
                modeMask = (intValue / 100).digits().map((ParameterMode)::get).toList()
            )
        }
    }

    private enum class ParameterMode {
        POSITION,
        IMMEDIATE,
        RELATIVE;

        companion object {

            private val values = values()

            operator fun get(ordinal: Long): ParameterMode = values[ordinal.toInt()]
        }
    }

    enum class Operation(val code: Long) {
        ADD(code = 1),
        MULTIPLY(code = 2),
        INPUT(code = 3),
        OUTPUT(code = 4),
        JUMP_IF_TRUE(code = 5),
        JUMP_IF_FALSE(code = 6),
        LESS_THAN(code = 7),
        EQUALS(code = 8),
        RELATIVE_BASE_OFFSET(code = 9),
        HALT(code = 99);

        companion object {

            private val values = values()

            fun from(code: Long): Operation = values.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown code: $code")
        }
    }

    /**
     * Returns digits of the receiver (using [radix]) in little-endian (ascending order).
     */
    private fun Long.digits(radix: Long = 10): Sequence<Long> = sequence {
        var value = this@digits
        do {
            yield(value % radix)
            value /= radix
        } while (value != 0L)
    }
}
