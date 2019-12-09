package wtf.log.xmas2019.day.day07

import com.google.common.collect.Collections2
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import wtf.log.xmas2019.Day
import java.io.BufferedReader
import kotlin.math.max

@Suppress("UnstableApiUsage", "EXPERIMENTAL_API_USAGE")
object Day07 : Day<List<Int>, Int, Int> {

    override fun parseInput(reader: BufferedReader): List<Int> = reader
        .readText()
        .trim()
        .splitToSequence(',')
        .map(String::toInt)
        .toList()

    override fun part1(input: List<Int>): Int = runBlocking {
        val machines = (0..4).map { Machine(input) }
        Collections2
            .orderedPermutations(0..4)
            .asFlow()
            .map { phases ->
                machines.foldIndexed(0) { index, accumulator, machine ->
                    val phase = phases[index]
                    val machineInput = Channel<Int>(capacity = 2).apply {
                        send(phase)
                        send(accumulator)
                    }
                    val machineOutput = Channel<Int>(capacity = 1)
                    machine.runToCompletion(machineInput, machineOutput)
                    machineOutput.receive()
                }
            }
            .reduce { accumulator, value -> max(accumulator, value) }
    }

    override fun part2(input: List<Int>): Int = runBlocking(SupervisorJob()) {
        val machines = (0..4).map { Machine(input) }
        Collections2
            .orderedPermutations(5..9)
            .asFlow()
            .map { phases ->
                val jobs = mutableListOf<Job>()
                val firstInput = Channel<Int>()
                val lastOutput = machines.foldIndexed(firstInput) { index, channel, machine ->
                    val nextChannel = Channel<Int>()
                    jobs += launch { machine.runToCompletion(channel, nextChannel) }
                    channel.send(phases[index])
                    nextChannel
                }
                firstInput.send(0)
                val result = Channel<Int>(Channel.CONFLATED)
                val consumeJob = launch {
                    lastOutput.consumeEach { value ->
                        result.send(value)
                        firstInput.send(value)
                    }
                }
                jobs.joinAll()
                consumeJob.cancel()
                result.close()
                result.receive()
            }
            .reduce { accumulator, value -> max(accumulator, value) }
    }

    private class Machine(
        program: List<Int>
    ) {

        private val program = program.toIntArray()
        private val memory = this.program.copyOf()
        private var pc = 0

        @Suppress("ControlFlowWithEmptyBody")
        suspend fun runToCompletion(
            input: ReceiveChannel<Int>,
            output: SendChannel<Int>
        ) {
            pc = 0
            program.copyInto(memory)
            while (cycle(input, output));
        }

        private suspend fun cycle(input: ReceiveChannel<Int>, output: SendChannel<Int>): Boolean {
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
                    val value = input.receive()
                    val resultAddress = memory[pc++]
                    memory[resultAddress] = value
                }
                Operation.OUTPUT -> {
                    val value = getArgument(instruction.modeMask, offset = 0)
                    output.send(value)
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
