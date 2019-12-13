package wtf.log.xmas2019.day.day11

import com.google.common.collect.TreeBasedTable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import wtf.log.xmas2019.Day
import wtf.log.xmas2019.util.collect.set
import wtf.log.xmas2019.util.flow.chunked
import java.io.BufferedReader

object Day11 : Day<List<Long>, Int, Day11.Grid> {

    override fun parseInput(reader: BufferedReader): List<Long> = reader
        .readText()
        .trim()
        .splitToSequence(',')
        .map(String::toLong)
        .toList()

    override fun part1(input: List<Long>): Int = runRobot(input, startingColor = null).size

    override fun part2(input: List<Long>): Grid = runRobot(input, startingColor = Color.WHITE)

    @UseExperimental(FlowPreview::class)
    private fun runRobot(input: List<Long>, startingColor: Color?): Grid = runBlocking {
        val grid = Grid()
        val machine = Machine(input)
        val inputChannel = Channel<Long>()
        val outputChannel = Channel<Long>()
        val machineJob = launch {
            machine.runToCompletion(inputChannel, outputChannel)
        }

        var position = Point(x = 0, y = 0)
        var orientation = Orientation.UP
        if (startingColor != null) {
            grid[position] = startingColor
        }
        val robotJob = launch {
            outputChannel
                .consumeAsFlow()
                .chunked(size = 2)
                .collect { (colorOrdinal, turnDirection) ->
                    grid[position] = Color.from(colorOrdinal)
                    orientation = orientation.turning(turnDirection)
                    position = position.toward(orientation)
                    inputChannel.send(grid[position].ordinal.toLong())
                }
        }
        inputChannel.send(grid[position].ordinal.toLong())
        machineJob.join()
        robotJob.cancelAndJoin()
        grid
    }

    enum class Color {
        BLACK,
        WHITE;

        companion object {

            private val values = values()

            fun from(ordinal: Long) = values[ordinal.toInt()]
        }
    }

    enum class Orientation(val deltaX: Int = 0, val deltaY: Int = 0) {
        LEFT(deltaX = -1),
        UP(deltaY = -1),
        RIGHT(deltaX = 1),
        DOWN(deltaY = 1);

        fun turning(direction: Long): Orientation {
            return values[(ordinal + ((direction * 2L) - 1L).toInt() + values.size) % values.size]
        }

        companion object {

            private val values = values()
        }
    }

    data class Point(val x: Int, val y: Int) {

        fun toward(orientation: Orientation) = Point(x = x + orientation.deltaX, y = y + orientation.deltaY)
    }

    class Grid {

        private val table = TreeBasedTable.create<Int, Int, Color>()

        val size: Int
            get() = table.size()

        operator fun get(point: Point): Color = get(point.x, point.y)

        operator fun get(x: Int, y: Int): Color = table[y, x] ?: Color.BLACK

        operator fun set(point: Point, value: Color) = set(point.x, point.y, value)

        operator fun set(x: Int, y: Int, value: Color) {
            table[y, x] = value
        }

        override fun toString(): String = buildString {
            val width = table.columnKeySet().size
            val height = table.rowKeySet().size
            append('┌')
            repeat(width) {
                append('─')
            }
            append("┐\n")
            repeat(height) { y ->
                append('│')
                repeat(width) { x ->
                    append(
                        when (get(x, y)) {
                            Color.BLACK -> '█'
                            Color.WHITE -> '░'
                        }
                    )
                }
                append("│\n")
            }
            append('└')
            repeat(width) {
                append('─')
            }
            append('┘')
        }
    }

    private class Machine(
        private val program: List<Long>
    ) {

        private val memory = mutableMapOf<Long, Long>()
        private var relativeBase = 0L
        private var pc = 0L

        @Suppress("ControlFlowWithEmptyBody")
        suspend fun runToCompletion(
            input: ReceiveChannel<Long>,
            output: SendChannel<Long>
        ) {
            pc = 0
            memory.clear()
            program.forEachIndexed { index, value ->
                memory[index.toLong()] = value
            }
            while (cycle(input, output));
        }

        private suspend fun cycle(
            input: ReceiveChannel<Long>,
            output: SendChannel<Long>
        ): Boolean {
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
                    writeResult(instruction.modeMask, offset = 2, value = multiplicand1 * multiplicand2)
                    pc += 3
                }
                Operation.INPUT -> {
                    val value = input.receive()
                    writeResult(instruction.modeMask, offset = 0, value = value)
                    pc++
                }
                Operation.OUTPUT -> {
                    val value = getArgument(instruction.modeMask, offset = 0)
                    output.send(value)
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
