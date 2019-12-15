package wtf.log.xmas2019.day.day13

import com.google.common.collect.TreeBasedTable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import wtf.log.xmas2019.Day
import wtf.log.xmas2019.util.collect.set
import wtf.log.xmas2019.util.flow.chunked
import wtf.log.xmas2019.util.math.digits
import java.io.BufferedReader
import kotlin.math.sign

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
object Day13 : Day<List<Long>, Int, Long> {

    override fun parseInput(reader: BufferedReader): List<Long> = reader
        .readText()
        .trim()
        .splitToSequence(',')
        .map(String::toLong)
        .toList()

    override fun part1(input: List<Long>): Int? = runBlocking {
        val channel = Channel<Long>()
        val machine = Machine(input)
        val grid = Grid()
        launch { machine.runToCompletion(channel) { 0L } }
        channel
            .consumeAsFlow()
            .chunked(size = 3)
            .collect { (x, y, ordinal) ->
                grid[x, y] = Tile.from(ordinal.toInt())
            }

        grid.count(Tile.BLOCK)
    }

    override fun part2(input: List<Long>): Long = runBlocking {
        val machine = Machine(input.toMutableList().apply { set(0, 2) })
        val output = Channel<Long>()
        val grid = Grid()
        var score = 0L
        val runJob = launch {
            machine.runToCompletion(output) {
                val paddlePosition = grid.paddlePosition!!
                val ballPosition = grid.ballPosition!!
                val velocityX = machine.memory.getValue(390L)
                val deltaX = ballPosition.x - paddlePosition.x
                val deltaY = ballPosition.y - paddlePosition.y
                when {
                    deltaX > 1L -> 1L
                    deltaX < -1L -> -1L
                    deltaX == 0L && deltaY == -1L -> 0L
                    else -> velocityX.sign.toLong()
                }
            }
        }
        val outputJob = launch {
            output
                .consumeAsFlow()
                .chunked(size = 3)
                .collect { (x, y, value) ->
                    if (x == -1L && y == 0L) score = value
                    else {
                        val point = Point(x, y)
                        val tile = Tile.from(value.toInt())
                        grid[point] = tile
                    }
                }
        }
        runJob.join()
        outputJob.cancelAndJoin()
        score
    }

    enum class Tile(val character: Char) {
        EMPTY(' '),
        WALL('█'),
        BLOCK('░'),
        PADDLE('#'),
        BALL('O');

        companion object {

            private val values = values()

            fun from(ordinal: Int): Tile = values[ordinal]
        }
    }

    data class Point(val x: Long, val y: Long)

    class Grid {

        private val table = TreeBasedTable.create<Long, Long, Tile>()

        val size: Int
            get() = table.size()

        var paddlePosition: Point? = null
            private set

        var ballPosition: Point? = null
            private set

        operator fun get(point: Point): Tile = get(point.x, point.y)

        operator fun get(x: Long, y: Long): Tile = table[y, x] ?: Tile.EMPTY

        operator fun set(point: Point, value: Tile) = set(point.x, point.y, value)

        operator fun set(x: Long, y: Long, value: Tile) {
            table[y, x] = value
            when (value) {
                Tile.PADDLE -> paddlePosition = Point(x, y)
                Tile.BALL -> ballPosition = Point(x, y)
                Tile.EMPTY,
                Tile.WALL,
                Tile.BLOCK -> {
                }
            }
        }

        fun count(tile: Tile): Int = table.values().count { it == tile }

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
                    append(get(x.toLong(), y.toLong()).character)
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

    @UseExperimental(ExperimentalCoroutinesApi::class)
    class Machine(
        private val program: List<Long>
    ) {

        private val _memory = mutableMapOf<Long, Long>()
        private var relativeBase = 0L
        private var pc = 0L

        val memory: Map<Long, Long>
            get() = _memory

        @Suppress("ControlFlowWithEmptyBody")
        suspend fun runToCompletion(
            output: SendChannel<Long>,
            input: () -> Long
        ) = coroutineScope {
            pc = 0
            _memory.clear()
            program.forEachIndexed { index, value ->
                _memory[index.toLong()] = value
            }
            while (cycle(input, output));
        }

        private suspend fun cycle(
            input: () -> Long,
            output: SendChannel<Long>
        ): Boolean {
            if (pc < 0) return false
            val instruction = Instruction
                .from(_memory.getOrElse(pc++) { 0L })
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
                    val value = input()
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
                    output.close()
                    return false
                }
            }
            return true
        }

        private fun getArgument(modeMask: List<ParameterMode>, offset: Long): Long {
            val mode = modeMask.getOrElse(offset.toInt()) {
                ParameterMode.POSITION
            }
            val argument = _memory.getOrElse(pc + offset) { 0L }
            return when (mode) {
                ParameterMode.POSITION -> _memory.getOrElse(argument) { 0L }
                ParameterMode.IMMEDIATE -> argument
                ParameterMode.RELATIVE -> _memory.getOrElse(relativeBase + argument) { 0L }
            }
        }

        private fun writeResult(modeMask: List<ParameterMode>, offset: Long, value: Long) {
            val mode = modeMask.getOrElse(offset.toInt()) {
                ParameterMode.POSITION
            }
            val argument = _memory.getOrElse(pc + offset) { 0L }
            when (mode) {
                ParameterMode.POSITION -> _memory[argument] = value
                ParameterMode.IMMEDIATE -> throw UnsupportedOperationException("Cannot write in immediate mode")
                ParameterMode.RELATIVE -> _memory[relativeBase + argument] = value
            }
        }

        data class Instruction(
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

        enum class ParameterMode {
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
    }
}
